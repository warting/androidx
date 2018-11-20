package org.jetbrains.kotlin.r4a

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.R4aFqNames.makeComposableAnnotation
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.ast.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * This class is used to resolve a KTX Element to the corresponding set of calls on the composer, and the individual calls for
 * each attribute, etc.
 */
class KtxCallResolver(
    private val callResolver: CallResolver,
    private val facade: ExpressionTypingFacade,
    private val project: Project
) {

    private class TempResolveInfo(
        val valid: Boolean,
        val trace: TemporaryTraceAndCache,
        val attributesLeft: Int,
        val usedAttributes: Set<String>,
        val missingRequiredAttributes: List<DeclarationDescriptor>,
        val build: () -> EmitOrCallNode
    )

    private class TempParameterInfo(
        val attribute: AttributeInfo,
        val descriptor: DeclarationDescriptor,
        val type: KotlinType
    )

    // NOTE(lmr): I am unsure of the performance consequences of using this. it appears to create a file for every
    // call, which seems like something we definitely do not want, but it's also used in resolving for(..) loops, so
    // maybe it's not terrible.
    private val psiFactory = KtPsiFactory(project, markGenerated = false)

    private val composableAnnotationChecker = ComposableAnnotationChecker()

    // The type of the `composer` variable in scope of the KTX tag
    private lateinit var composerType: KotlinType
    // A ResolvedCall to "get" the composer variable in scope of the KTX element.
    private lateinit var composerResolvedCall: ResolvedCall<*>
    // A ResolvedCall to the `joinKey(Any, Any?)` method on the composer in scope.
    private lateinit var joinKeyCall: ResolvedCall<*>

    private lateinit var infixOrCall: ResolvedCall<*>

    private lateinit var tagExpressions: List<KtExpression>

    // Set of valid upper bound types that were defined on the composer that can't have children
    // For android, this should be [View]
    private val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()

    private fun KotlinType.isEmittable() = emitSimpleUpperBoundTypes.any { isSubtypeOf(it) }

    // Set of valid upper bound types that were defined on the composer that can have children.
    // For android, this would be [ViewGroup]
    private val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()

    private fun KotlinType.isCompoundEmittable() = emitCompoundUpperBoundTypes.any { isSubtypeOf(it) }

    // The specification for `emit` on a composer allows for the `ctor` parameter to be a function type
    // with any number of parameters. We allow for these parameters to be used as parameters in the
    // Constructors that are emitted with a KTX tag. These parameters can be overridden with attributes
    // in the KTX tag, but if there are required parameters with a type that matches one declared in the
    // ctor parameter, we will resolve it automatically with the value passed in the `ctor` lambda.
    //
    // In order to do this resolution, we store a list of pairs of "upper bounds" to parameter types. For example,
    // the following emit call:
    //
    //      fun <T : View> emit(key: Any, ctor: (context: Context) -> T, update: U<T>.() -> Unit)
    //
    // would produce a Pair of [View] to [Context]
    private val emittableTypeToImplicitCtorTypes = mutableListOf<Pair<List<KotlinType>, Set<KotlinType>>>()

    private fun isImplicitConstructorParam(
        param: ValueParameterDescriptor,
        fn: CallableDescriptor
    ): Boolean {
        val returnType = fn.returnType ?: return false
        val paramType = param.type
        for ((upperBounds, implicitTypes) in emittableTypeToImplicitCtorTypes) {
            if (!implicitTypes.any { it.isSubtypeOf(paramType) }) continue
            if (!returnType.satisfiesConstraintsOf(upperBounds)) continue
            return true
        }
        return false
    }

    /**
     * KTX tags are defined to resolve a "composer" in the scope of the tag itself, and then the tag translates into a call
     * or a set of calls on that composer instance. This method should be called first, and will resolve the composer in scope
     * and record various pieces of metadata about the composer that will make resolving the tag possible. If it returns false
     * then something went wrong and you should not try and resolve the tag. If the method returns false, at least one
     * diagnostic will have been added to the tag somewhere to indicate that there was a problem.
     */
    fun resolveComposer(element: KtxElement, context: ExpressionTypingContext): Boolean {

        // we want to report errors on the tag names (open and closing), and not the entire element, since
        // that would be too noisy
        tagExpressions = listOfNotNull(
            element.simpleTagName,
            element.simpleClosingTagName,
            element.qualifiedTagName,
            element.qualifiedClosingTagName
        )

        // The composer is currently resolved as whatever is currently in scope with the name "composer".
        val resolvedComposer = resolveVar(KtxNameConventions.COMPOSER, element, context)

        if (!resolvedComposer.isSuccess) {
            R4AErrors.NO_COMPOSER_FOUND.report(context, tagExpressions)
            return false
        }

        composerResolvedCall = resolvedComposer.resultingCall

        val descriptor = composerResolvedCall.resultingDescriptor

        composerType = when (descriptor) {
            is PropertyDescriptor -> descriptor.type
            is VariableDescriptor -> descriptor.type
            // if composer isn't a property or variable, we don't currently know how to resolve it...
            else -> {
                R4AErrors.NO_COMPOSER_FOUND.report(context, tagExpressions)
                return false
            }
        }

        val emitCandidates = resolveComposerMethodCandidates(element, KtxNameConventions.EMIT, context)

        for (candidate in emitCandidates.map { it.candidateDescriptor }) {
            if (candidate.name != KtxNameConventions.EMIT) continue
            if (candidate !is SimpleFunctionDescriptor) continue
            val params = candidate.valueParameters
            // NOTE(lmr): we could report diagnostics on some of these? it seems strange to emit diagnostics about a function
            // that is not necessarily being used though. I think it's probably better to just ignore them here.

            // the signature of emit that we are looking for has 3 or 4 parameters
            if (params.size < 3 || params.size > 4) continue
            val ctorParam = params.find { it.name == KtxNameConventions.EMIT_CTOR_PARAMETER } ?: continue
            if (!ctorParam.type.isFunctionTypeOrSubtype) continue

            // the return type from the ctor param is the "upper bound" of the node type. It will often be a generic type with constraints.
            val upperBounds = ctorParam.type.getReturnTypeFromFunctionType().upperBounds()

            // the ctor param can have parameters itself, which we interpret as implicit parameter types that the composer knows how to
            // automatically provide to the component. In the case of Android Views, this is how we automatically provide Context.
            val implicitParamTypes = ctorParam.type.getValueParameterTypesFromFunctionType().map { it.type }

            for (implicitType in implicitParamTypes) {
                emittableTypeToImplicitCtorTypes.add(upperBounds to implicitParamTypes.toSet())
            }

            emitSimpleUpperBoundTypes.addAll(upperBounds)

            if (params.any { it.name == KtxNameConventions.EMIT_CHILDREN_PARAMETER }) {
                emitCompoundUpperBoundTypes.addAll(upperBounds)
            }
        }

        if (emitSimpleUpperBoundTypes.isEmpty()) {

            // if the composer has no valid `emit` candidates, but *does* valid `call` methods, we will report no errors.
            // It's strange, but it's possible for a composer that only handles `call` to be useful. To be sure, we first
            // look up all of the call candidates.
            val callCandidates = resolveComposerMethodCandidates(element, KtxNameConventions.EMIT, context)

            if (callCandidates.isEmpty()) {
                R4AErrors.INVALID_COMPOSER_IMPLEMENTATION.report(
                    context,
                    tagExpressions,
                    composerType,
                    "Couldn't find any valid `call(...)` or `emit(...)` methods"
                )
            }
        }

        val orName = Name.identifier("or")
        val left = psiFactory.createSimpleName("a")
        val right = psiFactory.createSimpleName("b")
        val oper = psiFactory.createSimpleName(orName.identifier)

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, left, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, right, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        infixOrCall = resolveInfixOr(context)

        joinKeyCall = resolveJoinKey(
            expressionToReportErrorsOn = tagExpressions.firstOrNull() ?: return false,
            context = context
        ) ?: run {
            R4AErrors.INVALID_COMPOSER_IMPLEMENTATION.report(
                context,
                tagExpressions,
                composerType,
                "Couldn't find valid method 'fun joinKey(Any, Any?): Any'"
            )
            return false
        }

        return true
    }

    /**
     * This call is the main function of this class, and will take in a KtxElement and return an object with all of the information
     * necessary to generate the code for the KTX tag. This method will always return a result, but the result may contain errors
     * and it is the responsibility of the consumer of this class to handle that properly.
     */
    fun resolve(
        element: KtxElement,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall {
        val openTagExpr = element.simpleTagName ?: element.qualifiedTagName ?: error("shouldn't happen")
        val closeTagExpr = element.simpleClosingTagName ?: element.qualifiedClosingTagName
        val attributes = element.attributes

        val tmpTraceAndCache = TemporaryTraceAndCache.create(context, "trace for ktx tag", element)

        val contextToUse = context.replaceTraceAndCache(tmpTraceAndCache)

        val attrInfos = mutableMapOf<String, AttributeInfo>()

        for (attr in attributes) {
            val name = attr.key.getReferencedName()
            if (attrInfos.contains(name)) {
                contextToUse.trace.reportFromPlugin(
                    R4AErrors.DUPLICATE_ATTRIBUTE.on(attr.key),
                    R4ADefaultErrorMessages
                )
            }
            attrInfos[name] = AttributeInfo(
                value = attr.value ?: attr.key,
                key = attr.key,
                name = name
            )
        }

        // The tag expression and the body expression are both implicitly types of "attributes" for the tag, but they
        // aren't explictly names. As a result, we put them into the `attrInfos` map with special keys
        element.bodyLambdaExpression?.let {
            attrInfos[CHILDREN_KEY] = AttributeInfo(
                value = it,
                key = null,
                name = CHILDREN_KEY
            )
        }

        attrInfos[TAG_KEY] = AttributeInfo(
            value = openTagExpr,
            key = null,
            name = TAG_KEY
        )

        val usedAttributes = mutableSetOf<String>()

        val missingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

        // we want to resolve all reference targets on the open tag on the closing tag as well, but we don't want
        // to have to execute the resolution code for both the open and close each time, so we create a binding
        // trace that will observe for traces on the open tag and copy them over to the closing tag if one
        // exists. We choose to only use this trace when we know that useful slices might show up on the tag.
        var traceForOpenClose: BindingTrace = tmpTraceAndCache.trace
        closeTagExpr?.let {
            traceForOpenClose = referenceCopyingTrace(openTagExpr, closeTagExpr, tmpTraceAndCache.trace)
        }
        val receiver = resolveReceiver(openTagExpr, contextToUse.replaceBindingTrace(traceForOpenClose))

        val emitOrCall = resolveChild(
            openTagExpr,
            ResolveStep.Root(openTagExpr, closeTagExpr),
            makeCall(
                openTagExpr,
                receiver = receiver,
                calleeExpression = when (openTagExpr) {
                    is KtQualifiedExpression -> openTagExpr.selectorExpression
                    is KtSimpleNameExpression -> openTagExpr
                    else -> null
                }
            ),
            attrInfos,
            usedAttributes,
            missingRequiredAttributes,
            contextToUse
        )

        // TODO(lmr): validate that if it bottoms out at an emit(...) that it doesn't have any call(...)s

        emitOrCall.errorNode()?.let { error ->
            when (error) {
                is ErrorNode.NonCallableRoot -> {
                    val type = facade.getTypeInfo(openTagExpr, context.withThrowawayTrace(openTagExpr)).type

                    if (type != null) {
                        R4AErrors.INVALID_TAG_TYPE.report(
                            contextToUse,
                            tagExpressions,
                            type,
                            emitSimpleUpperBoundTypes
                        )
                    } else {
                        R4AErrors.INVALID_TAG_DESCRIPTOR.report(
                            contextToUse,
                            tagExpressions,
                            emitSimpleUpperBoundTypes
                        )
                    }
                }
                is ErrorNode.NonEmittableNonCallable -> {
                    // TODO(lmr): diagnostic
                    // "ktx tag terminated with type "Foo", which is neither an emittable, nor callable
                    R4AErrors.INVALID_TAG_TYPE.report(
                        contextToUse,
                        tagExpressions,
                        error.type,
                        emitSimpleUpperBoundTypes
                    )
                }
                is ErrorNode.RecursionLimitAmbiguousAttributesError -> {
                    R4AErrors.AMBIGUOUS_ATTRIBUTES_DETECTED.report(contextToUse, tagExpressions, error.attributes)
                }
                is ErrorNode.RecursionLimitError -> {
                    R4AErrors.CALLABLE_RECURSION_DETECTED.report(contextToUse, tagExpressions)
                }
            }
        }

        val constantChecker = ConstantExpressionEvaluator(
            project = project,
            module = contextToUse.scope.ownerDescriptor.module,
            languageVersionSettings = contextToUse.languageVersionSettings
        )

        val attributeNodes = emitOrCall
            .allAttributes()
            .mapNotNull { it as? AttributeNode }
            .groupBy { it.name }

        // we want to return a list of the used "AttributeNodes" to the top level call object that we return,
        // so we dig through all of the attributes of the AST and return a unique list. For efficiency, whether
        // or not an AttributeNode is "static" or not is not determined until now, since we don't want to have to
        // calculate it multiple times for the same attribute, so as we loop through this list, we calculate
        // whether it is static just once and then update all of the others as we go through them.
        val usedAttributeNodes = attributeNodes
            .mapValues { it.value.first() }
            .values
            .map { node ->
                val attr = attrInfos[node.name] ?: error("could not find attribute ${node.name}")
                val static = isStatic(attr.value, contextToUse, node.type, constantChecker)

                // update all of the nodes in the AST as "static"
                attributeNodes[node.name]?.forEach { it.isStatic = static }

                // return a node for the root of the AST that codegen can use
                AttributeNode(
                    name = node.name,
                    descriptor = node.descriptor,
                    expression = attr.value,
                    type = node.type,
                    isStatic = static
                )
            }

        // it's okay if the tag doesn't show up as used, so we remove it from this list
        val unusedAttributes = (attrInfos - usedAttributes - TAG_KEY).toMutableMap()

        if (unusedAttributes.isNotEmpty()) {

            // if we have some unused attributes, we want to provide some helpful diagnostics on them, so we grab
            // every possible attribute for the call. Note that we only want to run this (expensive) calculation in
            // cases where there *were* unused attributes, so the clean compile path should avoid this.
            val allPossibleAttributes = emitOrCall.allPossibleAttributes()

            loop@ for (attr in unusedAttributes.values) {
                when (attr.name) {
                    CHILDREN_KEY -> {
                        if (emitOrCall is EmitCallNode) {
                            val type = emitOrCall.memoize.ctorCall?.resultingDescriptor?.returnType ?: error("expected a return type")
                            if (!type.isCompoundEmittable()) {
                                contextToUse.trace.reportFromPlugin(
                                    R4AErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(openTagExpr),
                                    R4ADefaultErrorMessages
                                )
                            } else {
                                // this is a compound emittable, so we will interpret the children block as just code to execute
                                unusedAttributes.remove(CHILDREN_KEY)
                            }
                        } else {
                            val possibleChildren = allPossibleAttributes[CHILDREN_KEY] ?: emptyList()
                            if (possibleChildren.isNotEmpty()) {
                                contextToUse.trace.reportFromPlugin(
                                    R4AErrors.UNRESOLVED_CHILDREN.on(openTagExpr, possibleChildren.map { it.type }),
                                    R4ADefaultErrorMessages
                                )
                            } else {
                                contextToUse.trace.reportFromPlugin(
                                    R4AErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(openTagExpr),
                                    R4ADefaultErrorMessages
                                )
                            }
                        }
                    }
                    else -> {
                        val key = attr.key ?: error("expected non-null key expression")
                        val valueType = facade.getTypeInfo(attr.value, contextToUse).type

                        val descriptors = emitOrCall.resolvedCalls().flatMap {
                            listOfNotNull(
                                it.resultingDescriptor,
                                it.resultingDescriptor.returnType?.let { t ->
                                    if (t.isUnit()) null
                                    else t.constructor.declarationDescriptor
                                }
                            )
                        }

                        // since extension functions won't show up if we just traverse the member scope of the types in our call,
                        // we might be giving inaccurate diagnostics around what types are accepted for this attribute. Since we have
                        // the name, we can search for all possible candidates of attributes on a given node. This is expensive, but
                        // since we are only in the erroneous path, it shouldn't be a big deal.
                        val attrsOfSameKey = resolveAttributeCandidatesGivenNameAndNode(
                            emitOrCall,
                            attr.name,
                            context.withThrowawayTrace(openTagExpr)
                        )

                        if (attrsOfSameKey.isNotEmpty()) {
                            // NOTE(lmr): it would be great if we could record multiple possible types here instead of just one for
                            // autocomplete
                            contextToUse.trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, attr.value, attrsOfSameKey.first().type)
                        }

                        val diagnostic = when {
                            attrsOfSameKey.isNotEmpty() && valueType != null ->
                                R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(key, valueType, attrsOfSameKey.map { it.type })
                            attrsOfSameKey.isEmpty() && valueType != null ->
                                R4AErrors.UNRESOLVED_ATTRIBUTE_KEY.on(key, descriptors, attr.name, valueType)
                            attrsOfSameKey.isNotEmpty() && valueType == null ->
                                R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                    key,
                                    ErrorUtils.createErrorType("???"),
                                    attrsOfSameKey.map { it.type })
                            else ->
                                R4AErrors.UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE.on(key, descriptors, attr.name)
                        }

                        contextToUse.trace.reportFromPlugin(diagnostic, R4ADefaultErrorMessages)
                    }
                }
            }
        }

        if (missingRequiredAttributes.isNotEmpty()) {
            missingRequiredAttributes
                .filter { !it.hasChildrenAnnotation() }
                .ifNotEmpty {
                    R4AErrors.MISSING_REQUIRED_ATTRIBUTES.report(contextToUse, tagExpressions, this)
                }
            missingRequiredAttributes
                .filter { it.hasChildrenAnnotation() }
                .ifNotEmpty {
                    R4AErrors.MISSING_REQUIRED_CHILDREN.report(contextToUse, tagExpressions, first().typeAsAttribute())
                }
        }

        // for each attribute we've consumed, we want to go through and call `checkType` so that the type system can flow through to
        // all of the attributes with the right type information (now that we know what types the attributes should have).
        for (name in usedAttributes) {
            val expr = attrInfos[name]?.value ?: continue
            var type = usedAttributeNodes.find { it.name == name }?.type
            if (type == null && name == CHILDREN_KEY) {
                type = functionType(
                    annotations = Annotations.create(listOf(makeComposableAnnotation(context.scope.ownerDescriptor.module)))
                )
            }

            facade.checkType(
                expr,
                contextToUse.replaceExpectedType(type)
            )
        }

        tmpTraceAndCache.commit()

        return ResolvedKtxElementCall(
            usedAttributes = usedAttributeNodes,
            unusedAttributes = unusedAttributes.keys.toList(),
            emitOrCall = emitOrCall,
            getComposerCall = composerResolvedCall,
            emitSimpleUpperBoundTypes = emitSimpleUpperBoundTypes,
            emitCompoundUpperBoundTypes = emitCompoundUpperBoundTypes,
            infixOrCall = infixOrCall
        )
    }

    private fun resolveAttributeCandidatesGivenNameAndNode(
        node: EmitOrCallNode,
        name: String,
        context: ExpressionTypingContext
    ): List<AttributeMeta> {
        val setterName = Name.identifier(R4aUtils.setterMethodFromPropertyName(name))
        val fakeSetterExpr = psiFactory.createSimpleName(setterName.asString())
        val fakePropertyExpr = psiFactory.createSimpleName(name)
        val contextToUse = context.replaceCollectAllCandidates(true)
        val resolvedCalls = node.resolvedCalls()

        val params = resolvedCalls
            .flatMap { it.resultingDescriptor.valueParameters }
            .filter { it.name.asString() == name }
            .mapNotNull {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        val types = resolvedCalls
            .mapNotNull { it.resultingDescriptor.returnType }
            .filter { !it.isUnit() }

        // setters, including extension setters
        val setters = types
            .flatMap { type ->
                val call = makeCall(
                    callElement = fakeSetterExpr,
                    calleeExpression = fakeSetterExpr,
                    receiver = TransientReceiver(type)
                )

                callResolver.resolveCallWithGivenName(
                    BasicCallResolutionContext.create(
                        contextToUse,
                        call,
                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
                    ),
                    call,
                    fakeSetterExpr,
                    setterName
                ).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? SimpleFunctionDescriptor }
            .mapNotNull {
                when {
                    it.valueParameters.size != 1 -> null
                    it.returnType?.isUnit() == false -> null
                    else -> AttributeMeta(
                        name = name,
                        type = it.valueParameters.first().type,
                        isChildren = it.hasChildrenAnnotation(),
                        descriptor = it
                    )
                }
            }

        val properties = types
            .flatMap { type ->
                val call = CallMaker.makePropertyCall(TransientReceiver(type), null, fakePropertyExpr)

                val contextForVariable = BasicCallResolutionContext.create(
                    contextToUse,
                    call,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
                )

                callResolver.resolveSimpleProperty(contextForVariable).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? PropertyDescriptor }
            .map {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        return params + setters + properties
    }

    // pure. check where used
    private fun shouldMemoizeResult(resolvedCall: ResolvedCall<*>): Boolean {
        val descriptor = resolvedCall.resultingDescriptor
        val returnType = descriptor.returnType ?: builtIns.unitType
        val typeDescriptor = returnType.constructor.declarationDescriptor
        return returnType.hasMemoizableAnnotation() ||
                descriptor.hasMemoizableAnnotation() ||
                (typeDescriptor?.hasMemoizableAnnotation() ?: false)
    }

    private fun resolveTagValidations(
        kind: ComposerCallKind,
        step: ResolveStep,
        isStaticCall: Boolean,
        resolvedCall: ResolvedCall<*>,
        receiverScope: KotlinType,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (step !is ResolveStep.Root) return emptyList()
        if (isStaticCall) return emptyList()
        val descriptor = resolvedCall.resultingDescriptor
        when (resolvedCall.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                val receiver = resolvedCall.dispatchReceiver as? ExpressionReceiver ?: return emptyList()
                return listOf(
                    ValidatedAssignment(
                        validationType = ValidationType.CHANGED,
                        validationCall = resolveValidationCall(
                            kind = kind,
                            validationType = ValidationType.CHANGED,
                            attrType = receiver.type,
                            expressionToReportErrorsOn = receiver.expression,
                            receiverScope = receiverScope,
                            assignmentReceiverScope = null,
                            valueExpr = receiver.expression,
                            context = context
                        ).first,
                        assignment = null,
                        assignmentLambda = null,
                        attribute = AttributeNode(
                            name = TAG_KEY,
                            isStatic = false,
                            type = receiver.type,
                            expression = receiver.expression,
                            descriptor = descriptor
                        )
                    )
                )
            }
            else -> return emptyList()
        }
    }

    // Loop through all of the validated assignments for the child call and create validations for the parent call.
    // The validations from the child call should be converted into CHANGED validations because if they were set/update we
    // don't want to do anything but we do want changes in them to invalidate the whole group.
    private fun collectValidations(
        kind: ComposerCallKind,
        current: List<ValidatedAssignment>,
        children: List<AttributeNode>,
        expression: KtExpression,
        invalidReceiverScope: KotlinType,
        attributes: Map<String, AttributeInfo>,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        val result = mutableMapOf<String, ValidatedAssignment>()

        current.forEach {
            result[it.attribute.name] = it
        }

        children.forEach {
            if (result.containsKey(it.name)) return@forEach
            val attr = attributes[it.name] ?: error("did not find attribute")
            result[it.name] = it.asChangedValidatedAssignment(
                kind = kind,
                expressionToReportErrorsOn = attr.key ?: expression,
                receiverScope = invalidReceiverScope,
                valueExpr = attr.value,
                context = context
            )
        }

        return result.values.toList()
    }

    private fun resolveChild(
        expression: KtExpression,
        resolveStep: ResolveStep,
        call: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): EmitOrCallNode {
        if (!resolveStep.canRecurse()) {
            return when (resolveStep) {
                is ResolveStep.Root -> error("should never happen")
                is ResolveStep.Nested -> {
                    resolveStep.constructNonMemoizedCallLinkedList()?.apply { nextCall = resolveStep.errorNode } ?: resolveStep.errorNode!!
                }
            }
        }
        val tmpForCandidates = TemporaryTraceAndCache.create(
            context, "trace to resolve ktx element", expression
        )
        val results = getCandidates(resolveStep, call, context.replaceTraceAndCache(tmpForCandidates))

        if (results.allCandidates?.size == 0) {
            return when (resolveStep) {
                is ResolveStep.Root -> {
                    // if the root tag failed to resolve to anything, then the tag isn't even callable. The call resolver will
                    // add useful diagnostics in this case that we'd like to use, but it will only do it when we are not in
                    // the "collectAllCandidates" mode. We just call `getCandidates` again to put all of the diagnostics on the element
                    // that we want.
                    getCandidates(resolveStep, call, context, collectAllCandidates = false)
                    ErrorNode.NonCallableRoot()
                }
                is ResolveStep.Nested -> {
                    val error = ErrorNode.NonEmittableNonCallable(resolveStep.calleeType)
                    resolveStep.constructNonMemoizedCallLinkedList()?.apply { nextCall = error } ?: error
                }
            }
        }

        // TODO(lmr): we could have an optimization for results.isSuccess and attributes.size == 0 here

        val resolveInfos = results.allCandidates!!.mapNotNull { result ->
            val tmpForCandidate = TemporaryTraceAndCache.create(
                context, "trace to resolve ktx element", expression
            )

            var trace: BindingTrace = tmpForCandidate.trace

            if (resolveStep is ResolveStep.Root) {
                resolveStep.closeExpr?.let {
                    trace = referenceCopyingTrace(resolveStep.openExpr, resolveStep.closeExpr, tmpForCandidate.trace)
                }
                // not sure why this is needed, but it is
                copyReferences(
                    fromTrace = tmpForCandidates.trace,
                    toTrace = trace,
                    element = resolveStep.openExpr
                )
            }

            val candidateContext = context
                .replaceTraceAndCache(tmpForCandidate)
                .replaceBindingTrace(trace)

            val attrsUsedInCall = mutableSetOf<String>()

            val attrsUsedInSets = mutableSetOf<String>()

            val subMissingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

            val usedAttributeInfos = mutableListOf<TempParameterInfo>()

            val candidateResults = resolveCandidate(
                resolveStep,
                result,
                call,
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                subMissingRequiredAttributes,
                candidateContext
            )

            if (candidateResults.isNothing) return@mapNotNull TempResolveInfo(
                false,
                tmpForCandidate,
                (attributes - attrsUsedInCall).size,
                attrsUsedInCall,
                subMissingRequiredAttributes
            ) {
                ErrorNode.ResolveError()
            }

            val resolvedCall = candidateResults.resultingCalls.first()

            if (!candidateResults.isSuccess) {
                when (candidateResults.resultCode) {
                    OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH -> {
                        resolvedCall.call.valueArguments.map { resolvedCall.getArgumentMapping(it) }.forEach {
                            when (it) {
                                is ArgumentMatch -> {
                                    when (it.status) {
                                        ArgumentMatchStatus.TYPE_MISMATCH -> {
                                            val attr = attributes[it.valueParameter.name.asString()] ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type = facade.getTypeInfo(attr.value, candidateContext).type ?: return@forEach
                                            candidateContext.trace.reportFromPlugin(
                                                R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                R4ADefaultErrorMessages
                                            )
                                        }
                                        ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES -> {
                                            val attr = attributes[it.valueParameter.name.asString()] ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type = facade.getTypeInfo(attr.value, candidateContext).type ?: return@forEach

                                            candidateContext.trace.reportFromPlugin(
                                                R4AErrors.MISMATCHED_INFERRED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                R4ADefaultErrorMessages
                                            )
                                        }
                                        ArgumentMatchStatus.UNKNOWN -> {
                                            // NOTE(lmr): This can happen with the implicit constructor params. ignore it
                                        }
                                        ArgumentMatchStatus.SUCCESS -> {
                                            // do nothing
                                        }
                                        ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE -> {
                                            error("ARGUMENT_HAS_NO_TYPE")
                                        }
                                    }
                                }
                                is ArgumentUnmapped -> {
//                                    error("ArgumentUnmapped")
                                    return@mapNotNull TempResolveInfo(
                                        false,
                                        tmpForCandidate,
                                        (attributes - attrsUsedInCall).size,
                                        attrsUsedInCall,
                                        subMissingRequiredAttributes
                                    ) {
                                        ErrorNode.ResolveError()
                                    }
                                }
                            }
                        }
                    }
                    OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE,
                    OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES,
                    OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> {
                        return@mapNotNull TempResolveInfo(
                            false,
                            tmpForCandidate,
                            (attributes - attrsUsedInCall).size,
                            attrsUsedInCall,
                            subMissingRequiredAttributes
                        ) {
                            ErrorNode.ResolveError()
                        }
                    }
                    else -> {
                        error("new kind of resolution problem. figure out why this happened...")
                    }
                }
            }

            val returnType = resolvedCall.resultingDescriptor.returnType ?: builtIns.unitType

            val isStaticCall = isStaticTag(resolveStep, resolvedCall, candidateContext)

            val shouldMemoizeCtor = shouldMemoizeResult(resolvedCall)

            val nonMemoizedCall = NonMemoizedCallNode(
                resolvedCall = resolvedCall,
                params = resolvedCall.buildParamsFromAttributes(attributes),
                nextCall = null
            )

            if (returnType.isUnit()) {
                // bottomed out

                // it is important to pass in "result" here and not "resolvedCall" since "result" is the one that will have
                // the composable annotation on it in the case of lambda invokes
                val composability = composableAnnotationChecker.hasComposableAnnotation(candidateContext.trace, result)
                if (composability == ComposableAnnotationChecker.Composability.NOT_COMPOSABLE) {
                    candidateContext.trace.reportFromPlugin(
                        R4AErrors.NON_COMPOSABLE_INVOCATION.on(
                            expression,
                            "Lambda variable",
                            resolvedCall.primaryDescriptor
                        ),
                        R4ADefaultErrorMessages
                    )
                }

                return@mapNotNull TempResolveInfo(
                    true, // TODO(lmr): valid
                    tmpForCandidate,
                    (attributes - attrsUsedInCall).size,
                    attrsUsedInCall,
                    subMissingRequiredAttributes
                ) {
                    val composerCall = resolveComposerCall(
                        constructedType = null, // or should we pass in Unit here?
                        expressionToReportErrorsOn = expression,
                        context = candidateContext
                    ) ?: return@TempResolveInfo ErrorNode.ResolveError()

                    val invalidReceiverScope = composerCall
                        .resultingDescriptor
                        .valueParameters
                        .first { it.name == KtxNameConventions.CALL_INVALID_PARAMETER }
                        .type
                        .getReceiverTypeFromFunctionType() ?: error("Expected receiver type")

                    val tagValidations = resolveTagValidations(
                        kind = ComposerCallKind.CALL,
                        step = resolveStep,
                        isStaticCall = isStaticCall,
                        resolvedCall = resolvedCall,
                        receiverScope = invalidReceiverScope,
                        context = candidateContext
                    )

                    val pivotals = resolvePivotalAttributes(
                        attributes,
                        attrsUsedInCall,
                        usedAttributeInfos,
                        emptyList(),
                        returnType
                    )

                    if (resolveStep is ResolveStep.Root) {
                        MemoizedCallNode(
                            memoize = ComposerCallInfo(
                                context = candidateContext,
                                composerCall = composerCall,
                                pivotals = pivotals,
                                joinKeyCall = joinKeyCall,
                                ctorCall = null,
                                ctorParams = emptyList(),
                                validations = collectValidations(
                                    kind = ComposerCallKind.CALL,
                                    current = tagValidations,
                                    children = nonMemoizedCall.consumedAttributes(),
                                    expression = expression,
                                    attributes = attributes,
                                    invalidReceiverScope = invalidReceiverScope,
                                    context = candidateContext
                                )
                            ),
                            call = resolveStep.constructNonMemoizedCallLinkedList(nonMemoizedCall)
                        )
                    } else {
                        resolveStep.constructNonMemoizedCallLinkedList(nonMemoizedCall)
                    }
                }
            }

            if (returnType.isEmittable()) {

                val composerCall = resolveComposerEmit(
                    constructedType = returnType,
                    hasBody = attributes.contains(CHILDREN_KEY),
                    implicitCtorTypes = resolvedCall.call.valueArguments.mapNotNull { (it as? ImplicitCtorValueArgument)?.type },
                    expressionToReportErrorsOn = expression,
                    context = candidateContext
                ) ?: return@mapNotNull TempResolveInfo(
                    false,
                    tmpForCandidate,
                    (attributes - attrsUsedInCall - attrsUsedInSets).size,
                    attrsUsedInCall + attrsUsedInSets,
                    subMissingRequiredAttributes
                ) {
                    ErrorNode.ResolveError()
                }

                if (attributes.contains(CHILDREN_KEY) && returnType.isCompoundEmittable()) {
                    attrsUsedInSets.add(CHILDREN_KEY)
                }

                val updateReceiverScope = composerCall
                    .resultingDescriptor
                    .valueParameters
                    .first { it.name == KtxNameConventions.EMIT_UPDATER_PARAMETER }
                    .type
                    .getReceiverTypeFromFunctionType() ?: error("Expected receiver type")

                val setterValidations = resolveAllSetAttributes(
                    kind = ComposerCallKind.EMIT,
                    expressionToReportErrorsOn = expression,
                    receiverScope = updateReceiverScope,
                    type = returnType,
                    attributes = attributes,
                    attributesUsedInCall = attrsUsedInCall,
                    consumedAttributes = attrsUsedInSets,
                    missingRequiredAttributes = subMissingRequiredAttributes,
                    shouldMemoizeCtor = shouldMemoizeCtor,
                    context = candidateContext
                )

                val pivotals = resolvePivotalAttributes(
                    attributes,
                    attrsUsedInCall,
                    usedAttributeInfos,
                    setterValidations,
                    returnType
                )


                return@mapNotNull TempResolveInfo(
                    true,
                    tmpForCandidate,
                    (attributes - attrsUsedInCall - attrsUsedInSets).size,
                    attrsUsedInCall + attrsUsedInSets,
                    subMissingRequiredAttributes
                ) {
                    EmitCallNode(
                        memoize = ComposerCallInfo(
                            context = candidateContext,
                            composerCall = composerCall,
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = resolvedCall,
                            ctorParams = resolvedCall.buildParamsFromAttributes(attributes),
                            validations = setterValidations
                        )
                    )
                }
            }

            val composerCall = resolveComposerCall(
                constructedType = if (shouldMemoizeCtor) returnType else null,
                expressionToReportErrorsOn = expression,
                context = candidateContext
            ) ?: return@mapNotNull TempResolveInfo(
                false,
                tmpForCandidate,
                (attributes - attrsUsedInCall - attrsUsedInSets).size,
                attrsUsedInCall + attrsUsedInSets,
                subMissingRequiredAttributes
            ) {
                ErrorNode.ResolveError()
            }

            // the "invalid" lambda is at a different argument index depending on whether or not there is a "ctor" param.
            val invalidReceiverScope = composerCall
                .resultingDescriptor
                .valueParameters
                .first { it.name == KtxNameConventions.CALL_INVALID_PARAMETER }
                .type
                .getReceiverTypeFromFunctionType() ?: error("Expected receiver type")

            val tagValidations = resolveTagValidations(
                kind = ComposerCallKind.CALL,
                step = resolveStep,
                isStaticCall = isStaticCall,
                resolvedCall = resolvedCall,
                receiverScope = invalidReceiverScope,
                context = candidateContext
            )

            val setterValidations = resolveAllSetAttributes(
                kind = ComposerCallKind.CALL,
                expressionToReportErrorsOn = expression,
                receiverScope = invalidReceiverScope,
                type = returnType,
                attributes = attributes,
                attributesUsedInCall = attrsUsedInCall,
                consumedAttributes = attrsUsedInSets,
                missingRequiredAttributes = subMissingRequiredAttributes,
                shouldMemoizeCtor = shouldMemoizeCtor,
                context = candidateContext
            )

            val pivotals = resolvePivotalAttributes(
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                setterValidations,
                returnType
            )

            val attrsUsedInFollowingCalls = mutableSetOf<String>()

            val childCall = resolveChild(
                expression,
                resolveStep.recurse(
                    calleeType = returnType,
                    nonMemoizedCall = if (shouldMemoizeCtor) null else nonMemoizedCall,
                    isStaticCall = isStaticCall,
                    resolvedCall = resolvedCall,
                    specifiedAttributes = attrsUsedInCall + attrsUsedInSets
                ),
                makeCall(
                    psiFactory.createSimpleName("invoke"),
                    dispatchReceiver = TransientReceiver(returnType)
                ),
                attributes,
                attrsUsedInFollowingCalls,
                subMissingRequiredAttributes,
                candidateContext
            )

            val subUsedAttributes = attrsUsedInCall + attrsUsedInSets + attrsUsedInFollowingCalls

            val attrsLeft = (attributes - subUsedAttributes).size

            return@mapNotNull TempResolveInfo(
                true, // TODO(lmr): valid
                tmpForCandidate,
                attrsLeft,
                subUsedAttributes,
                subMissingRequiredAttributes
            ) {
                if (shouldMemoizeCtor || resolveStep is ResolveStep.Root) {
                    MemoizedCallNode(
                        memoize = ComposerCallInfo(
                            context = candidateContext,
                            composerCall = composerCall,
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = if (shouldMemoizeCtor) nonMemoizedCall.resolvedCall else null,
                            ctorParams = if (shouldMemoizeCtor) nonMemoizedCall.params else emptyList(),
                            validations = collectValidations(
                                kind = ComposerCallKind.CALL,
                                current = tagValidations + setterValidations,
                                children = childCall.consumedAttributes(),
                                expression = expression,
                                attributes = attributes,
                                invalidReceiverScope = invalidReceiverScope,
                                context = candidateContext
                            )
                        ),
                        call = childCall
                    )
                } else {
                    childCall
                }

            }
        }
            .sortedWith(Comparator { a, b ->
                if (a.attributesLeft != b.attributesLeft) {
                    a.attributesLeft - b.attributesLeft
                } else {
                    (if (a.valid) 0 else 1) - (if (b.valid) 0 else 1)
                }
            }).toList()

        val result = resolveInfos.first()

        val resultNode = result.build()
        usedAttributes.addAll(result.usedAttributes)
        missingRequiredAttributes.addAll(result.missingRequiredAttributes)
        result.trace.commit()

        if (resolveInfos.size > 1) {
            val nextBest = resolveInfos[1]
            if (result.attributesLeft == 0 && result.attributesLeft == nextBest.attributesLeft) {
                R4AErrors.AMBIGUOUS_KTX_CALL.report(
                    context,
                    tagExpressions,
                    resultNode,
                    nextBest.build()
                )
            }
        }
        return resultNode
    }

    private fun resolvePivotalAttributes(
        attributes: Map<String, AttributeInfo>,
        attrsUsedInCall: Set<String>,
        callParamInfos: List<TempParameterInfo>,
        validations: List<ValidatedAssignment>,
        returnType: KotlinType?
    ): List<AttributeNode> {
        val result = mutableListOf<AttributeNode>()

        if (returnType == null || returnType.isUnit()) {
            return callParamInfos
                .filter { it.descriptor.hasPivotalAnnotation() }
                .map {
                    AttributeNode(
                        name = it.attribute.name,
                        descriptor = it.descriptor,
                        type = it.type,
                        expression = it.attribute.value,
                        isStatic = false
                    )
                }
        }

        // if you were in the ctor call but not in the sets, you *have* to be pivotal
        for (info in callParamInfos) {
            if (attrsUsedInCall.contains(info.attribute.name)) continue
            val attribute = attributes[info.attribute.name] ?: continue
            result.add(
                AttributeNode(
                    name = info.attribute.name,
                    descriptor = info.descriptor,
                    type = info.type,
                    expression = attribute.value,
                    isStatic = false
                )
            )
        }

        // There are additional cases where attributes can be pivotal:
        //   1. It is annotated as @Pivotal
        //   2. It is a `val` ctor parameter
        for (assignment in validations) {
            val attribute = assignment.attribute
            val name = attribute.name
            val descriptor = attribute.descriptor

            if (descriptor.hasPivotalAnnotation()) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
            if (descriptor is PropertyDescriptor && attrsUsedInCall.contains(name) && !descriptor.isVar) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
        }

        return result
    }

    private fun ResolvedCall<*>.buildParamsFromAttributes(attributes: Map<String, AttributeInfo>): List<ValueNode> {
        return valueArguments.map { (param, value) ->
            val name = param.name.asString()
            var attr = attributes[name]

            if (value is DefaultValueArgument) {
                return@map DefaultValueNode(
                    name = name,
                    descriptor = param,
                    type = param.type
                )
            }

            if (param.hasChildrenAnnotation()) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
            }

            if (attr == null && isImplicitConstructorParam(param, resultingDescriptor)) {
                return@map ImplicitCtorValueNode(
                    name = name,
                    descriptor = param,
                    type = param.type
                )
            }

            if (attr == null) {
                error("Couldn't find attribute but expected to. param=$param name=$name")
            }

            AttributeNode(
                name = attr.name,
                isStatic = false,
                descriptor = param,
                type = param.type,
                expression = attr.value
            )
        }
    }

    // pure, can be moved out
    private fun AttributeNode.asChangedValidatedAssignment(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        valueExpr: KtExpression,
        context: ExpressionTypingContext
    ): ValidatedAssignment {
        val validationCall = resolveValidationCall(
            kind = kind,
            validationType = ValidationType.CHANGED,
            attrType = type,
            expressionToReportErrorsOn = expressionToReportErrorsOn,
            receiverScope = receiverScope,
            assignmentReceiverScope = null,
            valueExpr = valueExpr,
            context = context
        ).first

        return ValidatedAssignment(
            validationType = ValidationType.CHANGED,
            validationCall = validationCall,
            attribute = this,
            assignment = null,
            assignmentLambda = null
        )
    }

    private fun resolveAllSetAttributes(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        type: KotlinType?,
        attributes: Map<String, AttributeInfo>,
        attributesUsedInCall: Set<String>,
        consumedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        shouldMemoizeCtor: Boolean,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (type == null) return emptyList()
        val results = mutableListOf<ValidatedAssignment>()
        var children: AttributeInfo? = null
        for ((name, attribute) in attributes) {
            if (name == TAG_KEY) continue
            if (name == CHILDREN_KEY) {
                children = attribute
                continue
            }
            val keyExpr = attribute.key ?: error("key expected")

            val expectedTypes = mutableListOf<KotlinType>()

            var resolvedCall: ResolvedCall<*>? = null

            // NOTE(lmr): A ktx element that has access (like its a recursive call or a nested class) to the private property
            // of the tag will be able to set it as an attribute...  I'm not sure if that'a s good thing or not, but unless we
            // do something extra, that is indeed possible. Perhaps it's something we should look into.

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsSetter(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context
                )
            }

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsProperty(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context
                )
            }

            if (resolvedCall != null) {

                val validationType = when {
                    !shouldMemoizeCtor && attributesUsedInCall.contains(name) -> ValidationType.CHANGED
                    attributesUsedInCall.contains(name) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val (validationCall, lambdaDescriptor) = resolveValidationCall(
                    kind = kind,
                    expressionToReportErrorsOn = expressionToReportErrorsOn,
                    receiverScope = receiverScope,
                    assignmentReceiverScope = type,
                    validationType = validationType,
                    attrType = resolvedCall.resultingDescriptor.valueParameters.first().type,
                    valueExpr = attribute.value,
                    context = context
                )

                results.add(
                    ValidatedAssignment(
                        validationType = validationType,
                        assignment = resolvedCall,
                        assignmentLambda = lambdaDescriptor,
                        attribute = AttributeNode(
                            name = name,
                            expression = attribute.value,
                            type = resolvedCall.resultingDescriptor.valueParameters.first().type,
                            descriptor = resolvedCall.resultingDescriptor,
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
                consumedAttributes.add(name)
            }
        }

        if (children != null) {
            val expectedTypes = mutableListOf<KotlinType>()

            val childrenExpr = children.value as KtxLambdaExpression

            var resolvedCall: ResolvedCall<*>? = null

            for (descriptor in getChildrenDescriptors(type)) {
                if (resolvedCall != null) break

                when (descriptor) {
                    is PropertyDescriptor -> {
                        resolvedCall = resolveChildrenAsProperty(
                            type,
                            descriptor,
                            childrenExpr,
                            expectedTypes,
                            context
                        )
                    }
                    is SimpleFunctionDescriptor -> {
                        resolvedCall = resolveChildrenAsSetter(
                            type,
                            descriptor,
                            childrenExpr,
                            expectedTypes,
                            context
                        )
                    }
                }
            }
            if (resolvedCall != null) {

                val descriptor = resolvedCall.resultingDescriptor

                val validationType = when {
                    attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val attrName = when (descriptor) {
                    is SimpleFunctionDescriptor -> R4aUtils.propertyNameFromSetterMethod(descriptor.name.asString())
                    is PropertySetterDescriptor -> descriptor.correspondingProperty.name.asString()
                    else -> descriptor.name.asString()
                }

                attributes[attrName]?.let {
                    // they are providing a named attribute for a @Children attribute while also providing a children
                    // body. This is illegal.
                    context.trace.reportFromPlugin(
                        R4AErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(it.key!!, attrName),
                        R4ADefaultErrorMessages
                    )
                    consumedAttributes.add(attrName)
                }

                val attrType = descriptor.valueParameters.first().type

                val (validationCall, lambdaDescriptor) = resolveValidationCall(
                    kind = kind,
                    expressionToReportErrorsOn = expressionToReportErrorsOn,
                    receiverScope = receiverScope,
                    assignmentReceiverScope = type,
                    validationType = validationType,
                    attrType = attrType,
                    valueExpr = children.value,
                    context = context
                )

                results.add(
                    ValidatedAssignment(
                        validationType = when {
                            attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                            else -> ValidationType.SET
                        },
                        assignment = resolvedCall,
                        assignmentLambda = lambdaDescriptor,
                        attribute = AttributeNode(
                            name = CHILDREN_KEY,
                            expression = children.value,
                            type = resolvedCall.resultingDescriptor.valueParameters.first().type,
                            descriptor = resolvedCall.resultingDescriptor,
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
                consumedAttributes.add(CHILDREN_KEY)
            }
        }

        if (!type.isUnit()) {
            val cls = type.constructor.declarationDescriptor as? ClassDescriptor ?: error("unexpected classifier descriptor")
            val requiredAttributes = cls.unsubstitutedMemberScope
                .getContributedDescriptors()
                .mapNotNull { it as? PropertyDescriptor }
                // NOTE(lmr): I think we should consider not marking lateinit properties as required. It would maybe align
                // ourselves more with the language semantic of `lateinit`
                .filter { it.isLateInit && !Visibilities.isPrivate(it.visibility) }
                .filter { !it.hasHiddenAttributeAnnotation() }

            requiredAttributes
                .filter { !consumedAttributes.contains(it.name.asString()) }
                .filter { !it.hasChildrenAnnotation() }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }

            requiredAttributes
                .filter { it.hasChildrenAnnotation() }
                .filter { !consumedAttributes.contains(it.name.asString()) && !consumedAttributes.contains(CHILDREN_KEY) }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }
        }
        return results
    }

    // pure, can be moved out. used in resolveAllSetAttrs
    private fun getChildrenDescriptors(type: KotlinType): List<DeclarationDescriptor> {
        val descriptor = type.constructor.declarationDescriptor
        return when (descriptor) {
            is ClassDescriptor -> descriptor
                .unsubstitutedMemberScope
                .getContributedDescriptors()
                .filter { it.hasChildrenAnnotation() }
            else -> emptyList()
        }
    }

    private fun resolveAttributeAsSetter(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtReferenceExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val setterName = Name.identifier(R4aUtils.setterMethodFromPropertyName(name))
        val ambiguousReferences = mutableSetOf<DeclarationDescriptor>()

        if (valueExpr === keyExpr) {
            // punning...
            // punning has a single expression that both acts as reference to the value and to the property/setter. As a result, we do
            // two separate resolution steps, but we need to use BindingContext.AMBIGUOUS_REFERENCE_TARGET instead of
            // BindingContext.REFERENCE_TARGET, and since we can't unset the latter, we have to retrieve it from a temporary trace
            // and manually set the references later. Here we resolve the "reference to the value" and save it:
            val temporaryForPunning = TemporaryTraceAndCache.create(
                context, "trace to resolve reference for punning", keyExpr
            )

            facade.getTypeInfo(
                keyExpr,
                context.replaceTraceAndCache(temporaryForPunning)
            )

            temporaryForPunning.trace[BindingContext.REFERENCE_TARGET, keyExpr]?.let {
                ambiguousReferences.add(it)
            }

            temporaryForPunning.commit()
        }

        val receiver = TransientReceiver(instanceType)

        val call = makeCall(
            keyExpr,
            calleeExpression = keyExpr,
            valueArguments = listOf(CallMaker.makeValueArgument(valueExpr)),
            receiver = receiver
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", keyExpr
        )

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            keyExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (results.isAmbiguity || temporaryForFunction.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.addAll(
                results.resultingCalls.mapNotNull { it.resultingDescriptor.valueParameters.firstOrNull() }.map { it.type }
            )
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        if (valueExpr === keyExpr) {
            // punning...
            // we want to commit this trace, but filter out any REFERENCE_TARGET traces
            temporaryForFunction.trace.commit(
                { slice, value ->
                    !(value === valueExpr && (slice === BindingContext.REFERENCE_TARGET || slice === BindingContext.CALL))
                }, false
            )
            // TODO(lmr): even w/ ambiguous reference target, because we are setting a real reference target (which we really need to do
            // for codegen), the target of the actual descriptor doesn't show up...
            temporaryForFunction.cache.commit()
            ambiguousReferences.add(resolvedCall.resultingDescriptor)
            context.trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, keyExpr, ambiguousReferences)
        } else {
            // if we weren't punning, we can just commit like normal
            temporaryForFunction.commit()
        }

        return resolvedCall
    }

    private fun resolveAttributeAsProperty(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtSimpleNameExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val ambiguousReferences = mutableSetOf<DeclarationDescriptor>()

        if (valueExpr === keyExpr) {
            // punning...
            // punning has a single expression that both acts as reference to the value and to the property/setter. As a result, we do
            // two separate resolution steps, but we need to use BindingContext.AMBIGUOUS_REFERENCE_TARGET instead of
            // BindingContext.REFERENCE_TARGET, and since we can't unset the latter, we have to retrieve it from a temporary trace
            // and manually set the references later. Here we resolve the "reference to the value" and save it:
            val temporaryForPunning = TemporaryTraceAndCache.create(
                context, "trace to resolve reference for punning", keyExpr
            )

            facade.getTypeInfo(
                keyExpr,
                context.replaceTraceAndCache(temporaryForPunning)
            )

            temporaryForPunning.trace[BindingContext.REFERENCE_TARGET, keyExpr]?.let {
                ambiguousReferences.add(it)
            }
            temporaryForPunning.commit()
        }

        // NOTE(lmr): I'm not sure what the consequences are of using the tagExpr as the receiver...
        val receiver = TransientReceiver(instanceType)

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", keyExpr
        )

        val call = CallMaker.makePropertyCall(receiver, null, keyExpr)

        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.resolveSimpleProperty(contextForVariable)

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        val expectedType = (resolvedCall.resultingDescriptor as PropertyDescriptor).type

        facade.getTypeInfo(
            valueExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType(expectedType)
                .replaceCallPosition(CallPosition.PropertyAssignment(keyExpr))
        )

        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.add(expectedType)
            return null
        }

        val descriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        val setter = descriptor.setter ?: return null

        // NOTE(lmr): Without this, the value arguments don't seem to end up in the resolved call. I'm not
        // sure if there is a better way to do this or not but this seems to work okay.
        val setterCall = makeCall(
            resolvedCall.call.callElement,
            calleeExpression = resolvedCall.call.calleeExpression,
            receiver = resolvedCall.call.explicitReceiver,
            valueArguments = listOf(CallMaker.makeValueArgument(valueExpr))
        )

        val resolutionCandidate = ResolutionCandidate.create(
            setterCall, setter, resolvedCall.dispatchReceiver, resolvedCall.explicitReceiverKind, null
        )

        val resolvedSetterCall = ResolvedCallImpl.create(
            resolutionCandidate,
            TemporaryBindingTrace.create(context.trace, "Trace for fake property setter resolved call"),
            TracingStrategy.EMPTY,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, setterCall)
        )

        setterCall.valueArguments.forEachIndexed { index, arg ->
            resolvedSetterCall.recordValueArgument(
                setter.valueParameters[index],
                ExpressionValueArgument(arg)
            )
        }

        resolvedSetterCall.markCallAsCompleted()

        if (valueExpr === keyExpr) {
            // punning...
            temporaryForVariable.trace.commit(
                { slice, value ->
                    !(value === valueExpr && (slice === BindingContext.REFERENCE_TARGET || slice === BindingContext.CALL))
                }, false
            )
            temporaryForVariable.cache.commit()
            ambiguousReferences.add(descriptor)
            // TODO(lmr): even w/ ambiguous reference target, because we are setting a real reference target (which we really need to do
            // for codegen), the target of the actual descriptor doesn't show up...
            context.trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, keyExpr, ambiguousReferences)
        } else {
            temporaryForVariable.commit()
        }

        return resolvedSetterCall
    }

    private fun resolveChildrenAsSetter(
        instanceType: KotlinType,
        childrenDescriptor: SimpleFunctionDescriptor,
        childrenExpr: KtxLambdaExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val setterName = childrenDescriptor.name

        val valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr))
        val receiver = TransientReceiver(instanceType)
        val call = makeCall(
            childrenExpr,
            valueArguments = valueArguments,
            receiver = receiver,
            calleeExpression = childrenExpr // NOTE(lmr): this seems wrong
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", childrenExpr
        )

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            childrenExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (temporaryForFunction.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }
        // TODO(lmr): should we check isSuccess here or anything like that?

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        temporaryForFunction.commit()

        return resolvedCall
    }

    private fun resolveChildrenAsProperty(
        instanceType: KotlinType,
        propertyDescriptor: PropertyDescriptor,
        childrenExpr: KtxLambdaExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", childrenExpr
        )

        val receiver = TransientReceiver(instanceType)
        val call = makeCall(
            childrenExpr,
            calleeExpression = childrenExpr,
            receiver = receiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.computeTasksAndResolveCall<PropertyDescriptor>(
            contextForVariable,
            propertyDescriptor.name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        facade.getTypeInfo(
            childrenExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType((resolvedCall.resultingDescriptor).type)
                .replaceCallPosition(CallPosition.PropertyAssignment(null))
        )
        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }
        val descriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        val setter = descriptor.setter ?: return null

        // NOTE(lmr): Without this, the value arguments don't seem to end up in the resolved call. I'm not
        // sure if there is a better way to do this or not but this seems to work okay.
        val setterCall = makeCall(
            resolvedCall.call.callElement,
            calleeExpression = resolvedCall.call.calleeExpression,
            receiver = resolvedCall.call.explicitReceiver,
            valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr)) // TODO(lmr): check to see if adding this above to value arguments fixes it?????
        )

        val resolutionCandidate = ResolutionCandidate.create(
            setterCall, setter, resolvedCall.dispatchReceiver, resolvedCall.explicitReceiverKind, null
        )

        val resolvedSetterCall = ResolvedCallImpl.create(
            resolutionCandidate,
            TemporaryBindingTrace.create(context.trace, "Trace for fake property setter resolved call"),
            TracingStrategy.EMPTY,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, setterCall)
        )

        setterCall.valueArguments.forEachIndexed { index, arg ->
            resolvedSetterCall.recordValueArgument(
                setter.valueParameters[index],
                ExpressionValueArgument(arg)
            )
        }

        resolvedSetterCall.markCallAsCompleted()

        temporaryForVariable.commit()

        return resolvedSetterCall
    }

    private fun resolveCandidate(
        step: ResolveStep,
        candidate: ResolvedCall<FunctionDescriptor>,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        usedAttributeInfos: MutableList<TempParameterInfo>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val valueArguments = mutableListOf<ValueArgument>()

        val referencedDescriptor = candidate.resultingDescriptor

        val stableParamNames = referencedDescriptor.hasStableParameterNames()

        for (param in referencedDescriptor.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && param.hasChildrenAnnotation()) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)
                    arg = childrenAttr.toValueArgument(name, stableParamNames)

                    if (attr != null) {
                        // they are providing a named attribute for a @Children attribute while also providing a children
                        // body. This is illegal.
                        context.trace.reportFromPlugin(
                            R4AErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(attr.key!!, attr.name),
                            R4ADefaultErrorMessages
                        )
                        usedAttributes.add(attr.name)
                    }
                }
            }

            if (arg == null && attr != null) {
                usedAttributes.add(name)
                usedAttributeInfos.add(
                    TempParameterInfo(
                        attribute = attr,
                        descriptor = param,
                        type = param.type
                    )
                )
                context.trace.record(BindingContext.REFERENCE_TARGET, attr.key, param)
                arg = attr.toValueArgument(attr.name, stableParamNames)
            }

            if (arg == null && isImplicitConstructorParam(param, referencedDescriptor)) {
                arg = ImplicitCtorValueArgument(param.type)
            }

            if (arg != null) {
                valueArguments.add(arg)
            } else if (!param.declaresDefaultValue()) {
                // missing required parameter!
                missingRequiredAttributes.add(param)
            }
        }

        val call = makeCall(
            original.callElement,
            valueArguments = valueArguments,
            calleeExpression = original.calleeExpression,
            receiver = original.explicitReceiver,
            dispatchReceiver = original.dispatchReceiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        return when (step) {
            is ResolveStep.Root -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveStep.Nested -> forceResolveCallForInvoke(step.calleeType, contextForVariable)
        }
    }

    private fun getCandidates(
        step: ResolveStep,
        call: Call,
        context: ExpressionTypingContext,
        collectAllCandidates: Boolean = true
    ): OverloadResolutionResults<FunctionDescriptor> {
        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        ).replaceCollectAllCandidates(collectAllCandidates)

        return when (step) {
            is ResolveStep.Root -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveStep.Nested -> forceResolveCallForInvoke(step.calleeType, contextForVariable)
        }
    }

    private fun resolveReceiver(expression: KtExpression, context: ExpressionTypingContext): Receiver? {
        if (expression !is KtQualifiedExpression) return null
        val currentContext = context
            .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        expression.elementChain(currentContext)

        val receiverExpr = expression.receiverExpression

        val receiverTypeInfo = when (context.trace.get(BindingContext.QUALIFIER, receiverExpr)) {
            null -> facade.getTypeInfo(receiverExpr, currentContext)
            else -> KotlinTypeInfo(null, currentContext.dataFlowInfo)
        }

        // TODO(lmr): inspect jumps and nullability. We cant allow tags that can be null or return early
        val receiverType = receiverTypeInfo.type
            ?: ErrorUtils.createErrorType("Type for " + receiverExpr.text)

        return context.trace.get(BindingContext.QUALIFIER, receiverExpr)
            ?: ExpressionReceiver.create(receiverExpr, receiverType, context.trace.bindingContext)
    }

    private fun makeValueArgument(type: KotlinType, context: ExpressionTypingContext): ValueArgument {
        val fakeExpr = psiFactory.createSimpleName("tmpVar")

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, fakeExpr, KotlinTypeInfo(
                type = type,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        return CallMaker.makeValueArgument(fakeExpr)
    }

    private fun resolveJoinKey(
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        return resolveSubstitutableComposerMethod(
            KtxNameConventions.JOINKEY,
            listOf(
                builtIns.anyType,
                builtIns.anyType
            ),
            null,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveInfixOr(context: ExpressionTypingContext): ResolvedCall<*> {
        val orName = Name.identifier("or")
        val left = psiFactory.createSimpleName("a")
        val right = psiFactory.createSimpleName("b")
        val oper = psiFactory.createSimpleName(orName.identifier)

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, left, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, right, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        return callResolver.resolveCallWithGivenName(
            context,
            makeCall(
                callElement = left,
                calleeExpression = oper,
                receiver = ExpressionReceiver.create(left, builtIns.booleanType, context.trace.bindingContext),
                valueArguments = listOf(CallMaker.makeValueArgument(right))
            ),
            oper,
            Name.identifier("or")
        ).resultingCall
    }

    private fun resolveComposerEmit(
        implicitCtorTypes: List<KotlinType>,
        constructedType: KotlinType,
        hasBody: Boolean,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        return resolveSubstitutableComposerMethod(
            KtxNameConventions.EMIT,
            listOfNotNull(
                builtIns.anyType,
                functionType(
                    parameterTypes = implicitCtorTypes,
                    returnType = constructedType
                ),
                functionType(),
                if (hasBody) functionType() else null
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveComposerMethodCandidates(
        element: KtxElement,
        name: Name,
        context: ExpressionTypingContext
    ): Collection<ResolvedCall<*>> {
        val calleeExpression = psiFactory.createSimpleName(name.asString())

        val methodCall = makeCall(
            callElement = element,
            calleeExpression = calleeExpression,
            receiver = TransientReceiver(composerType)
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            methodCall,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, methodCall)
        )

        val results = callResolver.resolveCallWithGivenName(
            // it's important that we use "collectAllCandidates" so that extension functions get included
            contextForVariable.replaceCollectAllCandidates(true),
            methodCall,
            calleeExpression,
            name
        )

        return results.allCandidates ?: emptyList()
    }

    private fun resolveComposerCall(
        constructedType: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        // call signature is:
        // ==================
        // key: Any, invalid: V.() -> Boolean, block: () -> Unit
        // key: Any, ctor: () -> T, invalid: V.(T) -> Boolean, block: (T) -> Unit

        return resolveSubstitutableComposerMethod(
            KtxNameConventions.CALL,
            listOfNotNull(
                builtIns.anyType,
                constructedType?.let {
                    functionType(returnType = constructedType)
                },
                functionType(
                    parameterTypes = listOfNotNull(constructedType),
                    returnType = builtIns.booleanType
                ),
                functionType(parameterTypes = listOfNotNull(constructedType))
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveValidationCall(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        assignmentReceiverScope: KotlinType?,
        validationType: ValidationType,
        attrType: KotlinType,
        valueExpr: KtExpression,
        context: ExpressionTypingContext
    ): Pair<ResolvedCall<*>?, FunctionDescriptor?> {

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val name = validationType.name.toLowerCase()
        val includeLambda = validationType != ValidationType.CHANGED

        val calleeExpression = psiFactory.createSimpleName(name)

        // for call:
        // ValidatorType.set(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.update(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.changed(AttrType): Boolean

        // for emit:
        // ValidatorType.set(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.update(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.changed(AttrType): Unit

        val lambdaType = when {
            includeLambda && kind == ComposerCallKind.EMIT -> functionType(
                parameterTypes = listOf(attrType),
                receiverType = assignmentReceiverScope
            )
            includeLambda && kind == ComposerCallKind.CALL -> functionType(
                parameterTypes = listOf(attrType)
            )
            else -> null
        }
        val lambdaArg = lambdaType?.let { makeValueArgument(it, contextToUse) }
        val lambdaDescriptor = lambdaType?.let { createFunctionDescriptor(it, contextToUse) }

        val call = makeCall(
            callElement = expressionToReportErrorsOn,
            calleeExpression = calleeExpression,
            valueArguments = listOfNotNull(
                CallMaker.makeValueArgument(valueExpr),
                lambdaArg
            ),
            receiver = TransientReceiver(receiverScope)
        )

        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            calleeExpression,
            Name.identifier(name)
        )

        if (results.isSuccess) return results.resultingCall to lambdaDescriptor

        return null to null
    }

    private fun resolveSubstitutableComposerMethod(
        methodName: Name,
        argumentTypes: List<KotlinType>,
        typeToSubstitute: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val composerExpr = psiFactory.createSimpleName(methodName.asString())

        val call = makeCall(
            callElement = expressionToReportErrorsOn,
            calleeExpression = composerExpr,
            receiver = TransientReceiver(composerType),
            valueArguments = argumentTypes.map { makeValueArgument(it, contextToUse) }
        )

        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            composerExpr,
            methodName
        )

        if (results.isSuccess) return results.resultingCall

        if (typeToSubstitute == null) return null

        val candidates = if (context.collectAllCandidates) results.allCandidates ?: emptyList() else results.resultingCalls

        for (candidate in candidates) {

            val typeParam = candidate.typeArguments.keys.singleOrNull() ?: continue

            if (!typeToSubstitute.satisfiesConstraintsOf(typeParam)) continue

            val nextTempTrace = TemporaryTraceAndCache.create(
                context, "trace to resolve variable", expressionToReportErrorsOn
            )

            val nextContext = context
                .replaceTraceAndCache(nextTempTrace)
                .replaceCollectAllCandidates(false)

            val substitutor = TypeSubstitutor.create(
                mapOf(
                    typeParam.typeConstructor to typeToSubstitute.asTypeProjection()
                )
            )

            val nextCall = makeCall(
                callElement = expressionToReportErrorsOn,
                calleeExpression = composerExpr,
                receiver = TransientReceiver(composerType),
                valueArguments = candidate.candidateDescriptor.valueParameters.map { makeValueArgument(it.type, nextContext) }
            )

            val nextResults = callResolver.resolveCallWithKnownCandidate(
                nextCall,
                TracingStrategyImpl.create(composerExpr, nextCall),
                BasicCallResolutionContext.create(
                    nextContext,
                    nextCall,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
                ),
                ResolutionCandidate.create(
                    nextCall,
                    candidate.candidateDescriptor,
                    candidate.dispatchReceiver,
                    candidate.explicitReceiverKind,
                    substitutor
                ),
                DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
            )

            if (nextResults.isSuccess) {
                nextTempTrace.commit()
                return nextResults.resultingCall
            }
        }

        return if (context.collectAllCandidates) null
        else resolveSubstitutableComposerMethod(
            methodName,
            argumentTypes,
            typeToSubstitute,
            expressionToReportErrorsOn,
            context.replaceCollectAllCandidates(true)
        )
    }

    private fun resolveVar(
        name: Name,
        expr: KtExpression,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<CallableDescriptor> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expr
        )
        val call = makeCall(expr)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )
        return callResolver.computeTasksAndResolveCall<CallableDescriptor>(
            contextForVariable,
            name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )
    }

    private fun KtQualifiedExpression.elementChain(context: ExpressionTypingContext) {
        val moduleDescriptor = context.scope.ownerDescriptor.module
        val trace = context.trace
        val scopeForFirstPart = context.scope

        val path = asQualifierPartList()
        val firstPart = path.first()
        var currentDescriptor: DeclarationDescriptor? = scopeForFirstPart.findDescriptor(firstPart)
        currentDescriptor = currentDescriptor ?:
                moduleDescriptor.getPackage(FqName.topLevel(firstPart.name)).let { if (it.isEmpty()) null else it }

        if (currentDescriptor == null) return
        else storeSimpleNameExpression(firstPart.expression!!, currentDescriptor, trace)

        // TODO(lmr): we need to add visibility checks into this function...
        for (qualifierPartIndex in 1 until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    // TODO(lmr): i wonder if we could allow this for Ktx. Seems like a nice to have
                    is TypeAliasDescriptor -> // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                        null
                    is ClassDescriptor -> {
                        var next: DeclarationDescriptor? = null
                        next = next ?: currentDescriptor.unsubstitutedInnerClassesScope.findDescriptor(qualifierPart)
                        if (currentDescriptor.kind == ClassKind.OBJECT) {
                            next = next ?: currentDescriptor.unsubstitutedMemberScope.findDescriptor(qualifierPart)
                        }
                        val cod = currentDescriptor.companionObjectDescriptor
                        if (cod != null) {
                            next = next ?: cod.unsubstitutedMemberScope.findDescriptor(qualifierPart)
                        }
                        next = next ?: currentDescriptor.staticScope.findDescriptor(qualifierPart)
                        next
                    }
                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.findDescriptor(qualifierPart)
                        }
                    }
                    is VariableDescriptor -> {
                        currentDescriptor.type.memberScope.findDescriptor(qualifierPart)
                    }
                    else -> null
                }

            if (nextPackageOrClassDescriptor == null) return
            else storeSimpleNameExpression(qualifierPart.expression!!, nextPackageOrClassDescriptor, trace)

            currentDescriptor = nextPackageOrClassDescriptor
        }
    }

    private fun storeSimpleNameExpression(
        expression: KtSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)
        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, expression, trace)

        val qualifier = when (descriptor) {
            is PackageViewDescriptor -> PackageQualifier(expression, descriptor)
            is ClassDescriptor -> ClassQualifier(expression, descriptor)
            is TypeParameterDescriptor -> TypeParameterQualifier(expression, descriptor)
            is TypeAliasDescriptor -> descriptor.classDescriptor?.let {
                TypeAliasQualifier(expression, descriptor, it)
            }
            else -> null
        }

        if (qualifier != null) {
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }

    // callresolver extension
    private fun forceResolveCallForInvoke(
        calleeType: KotlinType,
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val fake = psiFactory.createSimpleName(OperatorNameConventions.INVOKE.asString())
        val expressionReceiver = ExpressionReceiver.create(fake, calleeType, context.trace.bindingContext)
        val call = CallTransformer.CallForImplicitInvoke(
            context.call.explicitReceiver, expressionReceiver, context.call,
            false
        )
        val tracingForInvoke = TracingStrategyForInvoke(fake, call, calleeType)
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
    }

    // callResolver extension
    private fun resolveCallForInvoke(
        context: BasicCallResolutionContext,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        return callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            context, OperatorNameConventions.INVOKE, tracing,
            NewResolutionOldInference.ResolutionKind.Invoke
        )
    }

}

// move these to naming conventions???
const val CHILDREN_KEY = "<children>"
const val TAG_KEY = "<tag>"

// general utils
// ==============================

private fun ExpressionTypingContext.withThrowawayTrace(expression: KtExpression) = replaceTraceAndCache(
    TemporaryTraceAndCache.create(
        this,
        "Throwaway trace and cache",
        expression
    )
)

private val builtIns = DefaultBuiltIns.Instance


private fun makeCall(
    callElement: KtElement,
    calleeExpression: KtExpression? = null,
    valueArguments: List<ValueArgument> = emptyList(),
    receiver: Receiver? = null,
    dispatchReceiver: ReceiverValue? = null
): Call {
    return object : Call {
        override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
        override fun getValueArgumentList(): KtValueArgumentList? = null
        override fun getTypeArgumentList(): KtTypeArgumentList? = null
        override fun getExplicitReceiver(): Receiver? = receiver
        override fun getCalleeExpression(): KtExpression? = calleeExpression
        override fun getValueArguments(): List<ValueArgument> = valueArguments
        override fun getCallElement(): KtElement = callElement
        override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
        override fun getTypeArguments(): List<KtTypeProjection> = emptyList()
        override fun getCallType(): Call.CallType = Call.CallType.DEFAULT
        override fun getCallOperationNode(): ASTNode? = null
    }
}

private fun functionType(
    parameterTypes: List<KotlinType> = emptyList(),
    annotations: Annotations = Annotations.EMPTY,
    returnType: KotlinType = builtIns.unitType,
    receiverType: KotlinType? = null
): KotlinType = createFunctionType(
    builtIns = builtIns,
    annotations = annotations,
    parameterNames = null,
    parameterTypes = parameterTypes,
    receiverType = receiverType,
    returnType = returnType
)

fun createFunctionDescriptor(
    type: KotlinType,
    context: ExpressionTypingContext
): FunctionDescriptor {
    return AnonymousFunctionDescriptor(
        context.scope.ownerDescriptor,
        Annotations.EMPTY,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE,
        false
    ).apply {
        initialize(
            type.getReceiverTypeFromFunctionType()?.let {
                DescriptorFactory.createExtensionReceiverParameterForCallable(this, it, Annotations.EMPTY)
            },
            null,
            emptyList(),
            type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                ValueParameterDescriptorImpl(
                    containingDeclaration = this,
                    original = null,
                    index = i,
                    annotations = Annotations.EMPTY,
                    name = t.type.extractParameterNameFromFunctionTypeArgument() ?: Name.identifier("p$i"),
                    outType = t.type,
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = SourceElement.NO_SOURCE
                )
            },
            type.getReturnTypeFromFunctionType(),
            Modality.FINAL,
            Visibilities.LOCAL,
            null
        )
        isOperator = false
        isInfix = false
        isExternal = false
        isInline = false
        isTailrec = false
        isSuspend = false
        isExpect = false
        isActual = false
    }
}

private fun KotlinType.satisfiesConstraintsOf(T: TypeParameterDescriptor): Boolean {
    return T.upperBounds.all { isSubtypeOf(it) }
}

private fun KotlinType.satisfiesConstraintsOf(bounds: List<KotlinType>): Boolean {
    return bounds.all { isSubtypeOf(it) }
}

// We want to return null in cases where types mismatch, so we use this heuristic to find out. I think there might be a more robust
// way to find this out, but I'm not sure what it would be
private fun BindingTrace.hasTypeMismatchErrorsOn(element: KtElement): Boolean =
    bindingContext.diagnostics.forElement(element).any { it.severity == Severity.ERROR }

private fun KtExpression.asQualifierPartList(): List<QualifiedExpressionResolver.QualifierPart> {
    val result = SmartList<QualifiedExpressionResolver.QualifierPart>()

    fun addQualifierPart(expression: KtExpression?): Boolean {
        if (expression is KtSimpleNameExpression) {
            result.add(QualifiedExpressionResolver.ExpressionQualifierPart(expression.getReferencedNameAsName(), expression))
            return true
        }
        return false
    }

    var expression: KtExpression? = this
    while (true) {
        if (addQualifierPart(expression)) break
        if (expression !is KtQualifiedExpression) break

        addQualifierPart(expression.selectorExpression)

        expression = expression.receiverExpression
    }

    return result.asReversed()
}

private fun HierarchicalScope.findDescriptor(part: QualifiedExpressionResolver.QualifierPart): DeclarationDescriptor? {
    return findFirstFromMeAndParent {
        it.findVariable(part.name, part.location)
            ?: it.findFunction(part.name, part.location)
            ?: it.findClassifier(part.name, part.location)
    }
}

private fun MemberScope.findDescriptor(part: QualifiedExpressionResolver.QualifierPart): DeclarationDescriptor? {
    return this.getContributedClassifier(part.name, part.location)
        ?: getContributedFunctions(part.name, part.location).singleOrNull()
        ?: getContributedVariables(part.name, part.location).singleOrNull()
}

private val ResolvedCall<*>.primaryDescriptor
    get() = when (this) {
        is VariableAsFunctionResolvedCall -> variableCall.candidateDescriptor
        else -> candidateDescriptor
    }

private fun KtExpression?.refExpressions(): List<KtReferenceExpression> = when (this) {
    is KtReferenceExpression -> listOf(this)
    is KtDotQualifiedExpression -> selectorExpression.refExpressions() + receiverExpression.refExpressions()
    else -> emptyList()
}

private fun KotlinType.upperBounds(): List<KotlinType> {
    return if (isTypeParameter()) {
        TypeUtils.getTypeParameterDescriptorOrNull(this)?.upperBounds ?: emptyList()
    } else {
        listOf(this)
    }
}

private fun AttributeInfo.toValueArgument(name: String, named: Boolean): ValueArgument {
    val argumentName = if (named) object : ValueArgumentName {
        override val asName: Name
            get() = Name.identifier(name)
        override val referenceExpression: KtSimpleNameExpression?
            get() = key
    } else null
    return object : ValueArgument {
        override fun getArgumentExpression() = value
        override fun getArgumentName() = argumentName
        override fun isNamed() = named
        override fun asElement(): KtElement = value
        override fun getSpreadElement(): LeafPsiElement? = null
        override fun isExternal() = true
    }
}

private fun DeclarationDescriptor.typeAsAttribute() = when (this) {
    is PropertyDescriptor -> type
    is ParameterDescriptor -> type
    is SimpleFunctionDescriptor -> valueParameters.first().type
    else -> error("unknown descriptor type")
}

// trace util
// ========================
private fun referenceCopyingTrace(from: KtExpression, to: KtExpression, trace: TemporaryBindingTrace): BindingTrace {
    val openTagExprs = from.refExpressions()
    val closeTagExprs = to.refExpressions()

    if (openTagExprs.size != closeTagExprs.size) return trace

    val elMap = openTagExprs.zip(closeTagExprs).toMap()

    val observableTrace = ObservableBindingTrace(trace)

    observableTrace.addHandler(BindingContext.REFERENCE_TARGET) { _, key, value ->
        val otherRefExpr = elMap[key]
        if (otherRefExpr != null) {
            trace.record(
                BindingContext.REFERENCE_TARGET,
                otherRefExpr,
                value
            )
        }
    }

    return observableTrace
}

private fun copyReferences(
    fromTrace: TemporaryBindingTrace,
    toTrace: BindingTrace,
    element: KtExpression
) {
    val references = element.refExpressions()
    val filter = TraceEntryFilter { slice, key ->
        slice === BindingContext.REFERENCE_TARGET && key in references
    }
    fromTrace.addOwnDataTo(toTrace, filter, false)
}

// util classes
// ========================
private class ImplicitCtorValueArgument(val type: KotlinType) : ValueArgument {
    override fun getArgumentExpression(): KtExpression? = null
    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): KtElement = error("tried to get element")
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = true
}

private class AttributeInfo(
    val value: KtExpression,
    val key: KtSimpleNameExpression?,
    val name: String
)

private sealed class ResolveStep(
    private val attributes: Set<String>,
    private val isValid: Boolean,
    private val trail: IntArray,
    val errorNode: ErrorNode?
) {
    class Root(
        val openExpr: KtExpression,
        val closeExpr: KtExpression?
    ) : ResolveStep(emptySet(), true, intArrayOf(1, 1, 1), null)

    class Nested(
        val calleeType: KotlinType,
        val nonMemoizedCall: NonMemoizedCallNode?,
        val isStaticCall: Boolean,
        val parent: ResolveStep,
        attributes: Set<String>,
        isValid: Boolean,
        trail: IntArray,
        errorNode: ErrorNode?
    ) : ResolveStep(attributes, isValid, trail, errorNode) {
        fun constructNonMemoizedCallLinkedList(): NonMemoizedCallNode? {
            return nonMemoizedCall?.let { parent.constructNonMemoizedCallLinkedList(it) }
        }
    }

    fun recurse(
        calleeType: KotlinType,
        nonMemoizedCall: NonMemoizedCallNode?,
        isStaticCall: Boolean,
        resolvedCall: ResolvedCall<*>,
        specifiedAttributes: Set<String>
    ): ResolveStep {
        val possibleAttributes = resolvedCall.resultingDescriptor.valueParameters.map { it.name.asString() }.toSet()
        var errorNode: ErrorNode? = null
        // steps in the recursion cannot define attributes that conflict with previous steps
        val intersection = attributes.intersect(possibleAttributes)
        val hasDuplicates = intersection.isNotEmpty()

        if (hasDuplicates) {
            // TODO(lmr): it would be nice if we also grabbed the descriptors that these attributes were on
            errorNode = ErrorNode.RecursionLimitAmbiguousAttributesError(intersection)
        }

        // we require that at least one of the last three steps has had an attribute that was used.
        // we could tweak this. Potentially the "hasDuplicates" test is more than enough to prevent
        // infinite recursion.
        val nextTrail = intArrayOf(trail[1], trail[2], specifiedAttributes.size)
        val trailIsValid = nextTrail.sum() > 0

        if (!trailIsValid) {
            errorNode = ErrorNode.RecursionLimitError()
        }

        return ResolveStep.Nested(
            calleeType = calleeType,
            nonMemoizedCall = nonMemoizedCall,
            isStaticCall = isStaticCall,
            parent = this,
            attributes = attributes + possibleAttributes,
            isValid = !hasDuplicates && trailIsValid,
            trail = nextTrail,
            errorNode = errorNode
        )
    }


    fun constructNonMemoizedCallLinkedList(nonMemoizedCall: NonMemoizedCallNode): NonMemoizedCallNode {
        var call = nonMemoizedCall
        var node = this
        while (node is ResolveStep.Nested) {
            val prevCall = node.nonMemoizedCall ?: break
            node = node.parent
            call = NonMemoizedCallNode(
                resolvedCall = prevCall.resolvedCall,
                params = prevCall.params,
                nextCall = call
            )
        }
        return call
    }

    fun canRecurse(): Boolean = isValid
}


// static checking
// ==========================
private fun isStatic(
    expression: KtExpression,
    context: ExpressionTypingContext,
    expectedType: KotlinType?,
    constantChecker: ConstantExpressionEvaluator
): Boolean {
    val constValue = constantChecker.evaluateExpression(expression, context.trace, expectedType)
    return constValue != null
}

private fun isStaticTag(step: ResolveStep, resolvedCall: ResolvedCall<*>, context: ExpressionTypingContext): Boolean {
    return when (step) {
        is ResolveStep.Root -> when (step.openExpr) {
            is KtQualifiedExpression -> {
                val parts = step.openExpr.asQualifierPartList()
                val targets = parts
                    .mapNotNull { it.expression }
                    .mapNotNull { context.trace[BindingContext.REFERENCE_TARGET, it] }

                if (parts.size != targets.size) return false

                val first = targets.first()

                if (!first.isRoot()) return false

                for (target in targets) {
                    val isValid = isValidStaticQualifiedPart(target)
                    if (!isValid)
                        return false
                }
                // TODO(lmr): is there more we need to do here?
                return true
            }
            is KtSimpleNameExpression -> {
                when (resolvedCall) {
                    is VariableAsFunctionResolvedCall -> {
                        val variableDescriptor = resolvedCall.variableCall.candidateDescriptor
                        if (variableDescriptor.isVar) return false
                        if (variableDescriptor.isConst) return true
                        val isRoot = variableDescriptor.isRoot()
                        when (variableDescriptor) {
                            is PropertyDescriptor -> (variableDescriptor.getter?.isDefault ?: false) && isRoot
                            else -> false
                        }
                    }
                    else -> true
                }
            }
            else -> false
        }
        is ResolveStep.Nested -> step.isStaticCall
    }
}

private fun isValidStaticQualifiedPart(target: DeclarationDescriptor): Boolean {
    return when (target) {
        is ClassDescriptor -> when {
            target.kind == ClassKind.OBJECT -> true
            target.isCompanionObject -> true
            else -> false
        }
        is ClassConstructorDescriptor -> true
        is PropertyDescriptor -> when {
            target.isVar -> false
            target.isConst -> true
            target.getter?.isDefault == true -> true
            else -> false
        }
        is FieldDescriptor -> isValidStaticQualifiedPart(target.correspondingProperty)
        is SimpleFunctionDescriptor -> true
        else -> {
            false
        }
    }
}

private fun DeclarationDescriptor.isRoot() = containingDeclaration?.containingDeclaration is ModuleDescriptor

enum class ComposerCallKind { CALL, EMIT }
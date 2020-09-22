/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.findFirstFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections

class DeepCopyIrTreeWithSymbolsPreservingMetadata(
    private val context: IrPluginContext,
    private val symbolRemapper: DeepCopySymbolRemapper,
    private val typeRemapper: TypeRemapper,
    private val typeTranslator: TypeTranslator,
    symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitClass(declaration: IrClass): IrClass {
        return super.visitClass(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        return super.visitSimpleFunction(declaration).also {
            it.correspondingPropertySymbol = declaration.correspondingPropertySymbol
            it.copyMetadataFrom(declaration)
        }
    }

    override fun visitField(declaration: IrField): IrField {
        return super.visitField(declaration).also {
            (it as IrFieldImpl).metadata = declaration.metadata
        }
    }

    override fun visitProperty(declaration: IrProperty): IrProperty {
        return super.visitProperty(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        return super.visitFile(declaration).also {
            if (it is IrFileImpl) {
                it.metadata = declaration.metadata
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        if (!expression.symbol.isBound)
            (context as IrPluginContextImpl).linker.getDeclaration(expression.symbol)
        val ownerFn = expression.symbol.owner as? IrConstructor
        // If we are calling an external constructor, we want to "remap" the types of its signature
        // as well, since if it they are @Composable it will have its unmodified signature. These
        // types won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            symbolRemapper.visitConstructor(ownerFn)
            val newFn = super.visitConstructor(ownerFn).also {
                it.parent = ownerFn.parent
                it.patchDeclarationParents(it.parent)
            }
            val newCallee = symbolRemapper.getReferencedConstructor(newFn.symbol)

            return IrConstructorCallImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                newCallee,
                expression.typeArgumentsCount,
                expression.constructorTypeArgumentsCount,
                expression.valueArgumentsCount,
                mapStatementOrigin(expression.origin)
            ).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }.copyAttributes(expression)
        }
        return super.visitConstructorCall(expression)
    }

    override fun visitCall(expression: IrCall): IrCall {
        val ownerFn = expression.symbol.owner as? IrSimpleFunction
        @Suppress("DEPRECATION")
        val containingClass = expression.symbol.descriptor.containingDeclaration as? ClassDescriptor

        // Any virtual calls on composable functions we want to make sure we update the call to
        // the right function base class (of n+1 arity). The most often virtual call to make on
        // a function instance is `invoke`, which we *already* do in the ComposeParamTransformer.
        // There are others that can happen though as well, such as `equals` and `hashCode`. In this
        // case, we want to update those calls as well.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            containingClass != null &&
            containingClass.defaultType.isFunctionType &&
            expression.dispatchReceiver?.type?.isComposable() == true
        ) {
            val typeArguments = containingClass.defaultType.arguments
            @Suppress("DEPRECATION")
            val newFnClass = context.symbolTable.referenceClass(
                context.builtIns
                    .getFunction(typeArguments.size)
            )
            @Suppress("DEPRECATION")
            val newDescriptor = newFnClass
                .descriptor
                .unsubstitutedMemberScope
                .findFirstFunction(ownerFn.name.identifier) { true }

            var newFn: IrSimpleFunction = IrFunctionImpl(
                ownerFn.startOffset,
                ownerFn.endOffset,
                ownerFn.origin,
                newDescriptor,
                expression.type
            )
            symbolRemapper.visitSimpleFunction(newFn)
            newFn = super.visitSimpleFunction(newFn).also { fn ->
                fn.parent = newFnClass.owner
                fn.overriddenSymbols = ownerFn.overriddenSymbols.map { it }
                fn.dispatchReceiverParameter = ownerFn.dispatchReceiverParameter
                fn.extensionReceiverParameter = ownerFn.extensionReceiverParameter
                newDescriptor.valueParameters.forEach { p ->
                    fn.addValueParameter(p.name.identifier, p.type.toIrType())
                }
                fn.patchDeclarationParents(fn.parent)
                assert(fn.body == null) { "expected body to be null" }
            }

            val newCallee = symbolRemapper.getReferencedSimpleFunction(newFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        // If we are calling an external function, we want to "remap" the types of its signature
        // as well, since if it is @Composable it will have its unmodified signature. These
        // functions won't be traversed by default by the DeepCopyIrTreeWithSymbols so we have to
        // do it ourself here.
        //
        // When an external declaration for a property getter/setter is transformed, we need to
        // also transform the corresponding property so that we maintain the relationship
        // `getterFun.correspondingPropertySymbol.owner.getter == getterFun`. If we do not
        // maintain this relationship inline class getters will be incorrectly compiled.
        if (
            ownerFn != null &&
            ownerFn.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        ) {
            if (ownerFn.correspondingPropertySymbol != null) {
                val property = ownerFn.correspondingPropertySymbol!!.owner
                symbolRemapper.visitProperty(property)
                super.visitProperty(property).also {
                    it.getter?.correspondingPropertySymbol = it.symbol
                    it.setter?.correspondingPropertySymbol = it.symbol
                    it.parent = ownerFn.parent
                    it.patchDeclarationParents(it.parent)
                }
            } else {
                symbolRemapper.visitSimpleFunction(ownerFn)
                super.visitSimpleFunction(ownerFn).also {
                    it.parent = ownerFn.parent
                    it.correspondingPropertySymbol = ownerFn.correspondingPropertySymbol
                    it.patchDeclarationParents(it.parent)
                }
            }
            val newCallee = symbolRemapper.getReferencedSimpleFunction(ownerFn.symbol)
            return shallowCopyCall(expression, newCallee).apply {
                copyRemappedTypeArgumentsFrom(expression)
                transformValueArguments(expression)
            }
        }

        return super.visitCall(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols, except with newCallee as a parameter */
    private fun shallowCopyCall(expression: IrCall, newCallee: IrSimpleFunctionSymbol): IrCall {
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newCallee,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
        }.copyAttributes(expression)
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun IrMemberAccessExpression.copyRemappedTypeArgumentsFrom(
        other: IrMemberAccessExpression
    ) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    /* copied verbatim from DeepCopyIrTreeWithSymbols */
    private fun <T : IrMemberAccessExpression> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
        when (this) {
            is IrPropertyImpl -> metadata = owner.metadata
            is IrFunctionBase<*> -> metadata = owner.metadata
            is IrClassImpl -> metadata = owner.metadata
        }
    }

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)
}

@Suppress("DEPRECATION")
class ComposerTypeRemapper(
    private val context: IrPluginContext,
    private val symbolRemapper: SymbolRemapper,
    private val typeTranslator: TypeTranslator,
    private val composerTypeDescriptor: ClassDescriptor
) : TypeRemapper {

    lateinit var deepCopy: IrElementTransformerVoid

    private val scopeStack = mutableListOf<IrTypeParametersContainer>()

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        scopeStack.add(irTypeParametersContainer)
    }

    override fun leaveScope() {
        scopeStack.pop()
    }

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    private val IrConstructorCall.annotationClass
        get() = this.symbol.descriptor.returnType.constructor.declarationDescriptor

    private fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.annotationClass?.fqNameOrNull() == fqName }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    private fun IrType.isFunction(): Boolean {
        val classifier = classifierOrNull ?: return false
        val name = classifier.descriptor.name.asString()
        if (!name.startsWith("Function")) return false
        classifier.descriptor.name
        return true
    }

    override fun remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type
        if (!type.isFunction()) return underlyingRemapType(type)
        if (!type.isComposable()) return underlyingRemapType(type)
        val oldIrArguments = type.arguments
        val realParams = oldIrArguments.size - 1
        var extraArgs = listOf(
            // composer param
            makeTypeProjection(
                composerTypeDescriptor.defaultType.replaceArgumentsWithStarProjections().toIrType(),
                Variance.INVARIANT
            )
        )
        val changedParams = changedParamCount(realParams, 1)
        extraArgs = extraArgs + (0 until changedParams).map {
            makeTypeProjection(context.irBuiltIns.intType, Variance.INVARIANT)
        }
        val newIrArguments =
            oldIrArguments.subList(0, oldIrArguments.size - 1) +
                extraArgs +
                oldIrArguments.last()

        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(
                context.symbolTable.referenceClass(
                    context
                        .irBuiltIns
                        .builtIns
                        .getFunction(oldIrArguments.size - 1 + extraArgs.size)
                )
            ),
            type.hasQuestionMark,
            newIrArguments.map { remapTypeArgument(it) },
            type.annotations.filter { !it.isComposableAnnotation() }.map {
                it.transform(deepCopy, null) as IrConstructorCall
            },
            null
        )
    }

    private fun underlyingRemapType(type: IrSimpleType): IrType {
        return IrSimpleTypeImpl(
            null,
            symbolRemapper.getReferencedClassifier(type.classifier),
            type.hasQuestionMark,
            type.arguments.map { remapTypeArgument(it) },
            type.annotations.map { it.transform(deepCopy, null) as IrConstructorCall },
            type.abbreviation?.remapTypeAbbreviation()
        )
    }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        if (typeArgument is IrTypeProjection)
            makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
        else
            typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.map { remapTypeArgument(it) },
            annotations
        )
}

private fun IrConstructorCall.isComposableAnnotation() =
    @Suppress("DEPRECATION")
    this.symbol.descriptor.returnType.constructor.declarationDescriptor?.fqNameSafe ==
        ComposeFqNames.Composable

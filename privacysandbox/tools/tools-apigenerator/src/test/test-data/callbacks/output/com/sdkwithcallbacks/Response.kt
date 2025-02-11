package com.sdkwithcallbacks

public data class Response(
    public val response: String,
    public val uiInterface: MyUiInterface,
    public val sharedUiInterface: MySharedUiInterface,
    public val myEnum: MyEnum,
)

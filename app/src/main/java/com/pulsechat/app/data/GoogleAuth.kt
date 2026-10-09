package com.pulsechat.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuth {

    suspend fun signIn(context: Context): GoogleIdTokenCredential {
        if (Supabase.GOOGLE_WEB_CLIENT_ID.startsWith("PUT_YOUR")) {
            throw RuntimeException("Google Web client ID is not set (see README).")
        }
        val credentialManager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Supabase.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data)
        }
        throw RuntimeException("Unexpected credential type")
    }
}

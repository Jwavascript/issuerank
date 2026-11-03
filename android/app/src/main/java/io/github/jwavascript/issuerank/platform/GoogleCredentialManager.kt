package io.github.jwavascript.issuerank.platform

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.GoogleClientId
import io.github.jwavascript.issuerank.data.GoogleCredential
import io.github.jwavascript.issuerank.data.GoogleCredentials

/**
 * "Google로 계속하기"의 계정 창(계획 2절 · D35): Credential Manager + GetSignInWithGoogleOption(serverClientId = 웹 클라이언트 ID · nonce).
 * Activity 문맥 = 계정 창을 그 화면 위에 띄움. 웹 클라이언트 ID가 자리표시자(C3)면 Credential Manager를 부르지 않고 Unavailable
 * (이 검사는 이 실제 구현 안에서만 — 시험의 가짜 걸이는 자리표시자와 상관없이 정상 흐름).
 * 토큰 · nonce는 돌려주기만 함(저장 · 로그 · 화면 출력 0).
 */
class GoogleCredentialManager(private val activity: Activity) : GoogleCredentials {
    override suspend fun idToken(nonce: String): GoogleCredential {
        val clientId = activity.getString(R.string.google_web_client_id)
        if (GoogleClientId.isPlaceholder(clientId)) return GoogleCredential.Unavailable
        val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId).setNonce(nonce).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleCredential.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleCredential.Failed
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleCredential.Cancelled
        } catch (e: NoCredentialException) {
            // 기기에 쓸 Google 계정이 없음 — 그 밖 실패와 같은 문구(웹 '완료하지 못했습니다')
            GoogleCredential.Failed
        } catch (e: GetCredentialException) {
            GoogleCredential.Failed
        } catch (e: GoogleIdTokenParsingException) {
            GoogleCredential.Failed
        }
    }
}

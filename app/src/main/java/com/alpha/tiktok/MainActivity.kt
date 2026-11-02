package com.alpha.tiktok

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var accountInput: EditText
    private lateinit var accountSpinner: Spinner
    private val prefName = "TikTokAccounts"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        urlInput = EditText(this).apply { hint = "رابط الفيديو"; setText("https://www.tiktok.com/") }
        layout.addView(urlInput)
        
        accountInput = EditText(this).apply { hint = "اسم الحساب الجديد" }
        layout.addView(accountInput)
        
        accountSpinner = Spinner(this)
        layout.addView(accountSpinner)
        updateSpinner()
        
        val btnOpen = Button(this).apply {
            text = "افتح الرابط"
            setOnClickListener { webView.loadUrl(urlInput.text.toString()) }
        }
        layout.addView(btnOpen)
        
        val btnSave = Button(this).apply {
            text = "حفظ الحساب الحالي"
            setOnClickListener { saveAccount() }
        }
        layout.addView(btnSave)
        
        val btnLoad = Button(this).apply {
            text = "تحميل الحساب"
            setOnClickListener { loadAccount() }
        }
        layout.addView(btnLoad)
        
        val btnLike = Button(this).apply {
            text = "إعجاب (Like)"
            setOnClickListener { doLike() }
        }
        layout.addView(btnLike)
        
        val btnComment = Button(this).apply {
            text = "تعليق (Comment)"
            setOnClickListener { doComment("🔥") }
        }
        layout.addView(btnComment)
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }
        layout.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        
        setContentView(layout)
        
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)
        webView.loadUrl("https://www.tiktok.com/")
    }

    private fun updateSpinner() {
        val prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val accounts = prefs.all.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accounts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accountSpinner.adapter = adapter
    }

    private fun saveAccount() {
        val name = accountInput.text.toString().trim()
        if (name.isEmpty()) return
        val cookies = CookieManager.getInstance().getCookie("https://www.tiktok.com/")
        if (cookies.isNullOrEmpty()) { Toast.makeText(this, "سجل الدخول أولاً", Toast.LENGTH_SHORT).show(); return }
        getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().putString(name, cookies).apply()
        updateSpinner()
        Toast.makeText(this, "تم حفظ: $name", Toast.LENGTH_SHORT).show()
    }

    private fun loadAccount() {
        val name = accountSpinner.selectedItem?.toString() ?: return
        val cookies = getSharedPreferences(prefName, Context.MODE_PRIVATE).getString(name, null) ?: return
        val cm = CookieManager.getInstance()
        cm.removeAllCookies {
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(webView, true)
            cookies.split(";").forEach { pair ->
                val parts = pair.trim().split("=")
                if (parts.size == 2) cm.setCookie("https://www.tiktok.com/", "${parts[0]}=${parts[1]}")
            }
            cm.flush()
            webView.loadUrl("https://www.tiktok.com/")
            Toast.makeText(this, "تم تحميل: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doLike() {
        val js = """
            (function(){
              var sels = ['[data-e2e="like-icon"]', '[data-e2e="browse-like-icon"]', 'button[aria-label*="Like"]'];
              for (var i=0; i<sels.length; i++){
                var el = document.querySelector(sels[i]);
                if (el){ var t = el.closest('button') || el; t.click(); return 'OK'; }
              }
              return 'FAIL';
            })()
        """.trimIndent()
        webView.evaluateJavascript(js) { result -> Toast.makeText(this, "النتيجة: $result", Toast.LENGTH_SHORT).show() }
    }

    private fun doComment(text: String) {
        val js = """
            (function(){
              var input = document.querySelector('[data-e2e="comment-input"], div[contenteditable="true"]');
              if (!input) return 'FAIL';
              input.focus();
              if (input.tagName.toLowerCase() === 'textarea') input.value = '$text'; else input.innerText = '$text';
              input.dispatchEvent(new Event('input', {bubbles:true}));
              var posts = ['[data-e2e="comment-post"]', 'button[type="submit"]'];
              for (var i=0; i<posts.length; i++){
                var b = document.querySelector(posts[i]);
                if (b && !b.disabled){ b.click(); return 'OK'; }
              }
              return 'FAIL';
            })()
        """.trimIndent()
        webView.evaluateJavascript(js) { result -> Toast.makeText(this, "النتيجة: $result", Toast.LENGTH_SHORT).show() }
    }
}

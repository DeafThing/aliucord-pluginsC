package cloudburst.plugins.sendembeds.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import com.aliucord.widgets.BottomSheet
import com.aliucord.widgets.LinearLayout
import com.aliucord.views.TextInput
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import android.view.inputmethod.EditorInfo
import com.aliucord.views.Button
import com.aliucord.utils.DimenUtils
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import com.discord.utilities.colors.ColorPickerUtils
import cloudburst.plugins.sendembeds.ui.ModeSelector

import com.aliucord.Utils
import com.aliucord.utils.GsonUtils

import cloudburst.plugins.sendembeds.utils.*
import cloudburst.plugins.sendembeds.SendEmbeds
import com.aliucord.Http

import com.discord.utilities.permissions.PermissionUtils
import com.discord.api.permission.Permission

import com.aliucord.utils.ReflectUtils
import com.discord.stores.StoreStream
import com.discord.stores.StorePermissions
import com.discord.utilities.rest.RestAPI

import com.discord.models.domain.NonceGenerator
import com.discord.utilities.time.ClockFactory

import com.discord.restapi.RestAPIParams
import com.aliucord.utils.RxUtils.createActionSubscriber
import com.aliucord.utils.RxUtils.subscribe
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import java.net.URLEncoder
import java.util.HashMap
import com.aliucord.Logger


fun View.setMarginEnd(
    value: Int
) {
    val params = Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT)
    params.gravity = Gravity.END
    params.bottomMargin = value
    this.layoutParams = params
}

class EmbedModal(val channelId: Long, val plugin: SendEmbeds, private val modeOverride: String?) : BottomSheet() {

    class EmptyTextWatcher(): TextWatcher {
        override public fun afterTextChanged(s: Editable) { }
        override public fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) { }
        override public fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) { }
    }

    private val logger = Logger("SendEmbeds")
    private var hasWebhookPermissions = false
    private var defaultWebhook: Webhook? = null
    private var allWebhooks = emptyArray<Webhook>()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val context = view.context
        val padding = DimenUtils.defaultPadding
        val p = padding / 2;
        this.setPadding(padding)

        val authorInput = TextInput(context, "Author").apply { 
            editText.apply {
                setText(StoreStream.getUsers().me.username)
                inputType = (EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE or EditorInfo.TYPE_CLASS_TEXT)
                imeOptions = EditorInfo.IME_ACTION_NEXT
            }
            setMarginEnd(p)
        }
        
        val titleInput = TextInput(context, "Title").apply { 
            editText.apply { 
                inputType = (EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE or EditorInfo.TYPE_CLASS_TEXT)
                imeOptions = EditorInfo.IME_ACTION_NEXT
            }
            setMarginEnd(p)
        }

        val contentInput = TextInput(context, "Content").apply { 
            editText.apply { 
                maxLines = Int.MAX_VALUE
                inputType = (EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE or 
                    EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or 
                    EditorInfo.TYPE_CLASS_TEXT)
                imeOptions = EditorInfo.IME_ACTION_DONE
                setHorizontallyScrolling(false)
            }
            setMarginEnd(p)
        }
        
        val urlInput = TextInput(context, "Url").apply { 
            editText.apply {
                inputType = (EditorInfo.TYPE_TEXT_VARIATION_URI or EditorInfo.TYPE_CLASS_TEXT)
                imeOptions = EditorInfo.IME_ACTION_NEXT
            }
            setMarginEnd(p)
        }
        
        val colorInput = TextInput(context, "Color", "#%06X".format(ColorCompat.getThemedColor(context, R.b.colorAccent) and 0x00FFFFFF), EmptyTextWatcher()).apply { 
            editText.apply { 
                inputType = EditorInfo.TYPE_NULL
                setOnClickListener {
                    val builder = ColorPickerUtils.INSTANCE.buildColorPickerDialog(
                        context, 
                        Utils.getResId("color_picker_title", "string"), 
                        ColorCompat.getThemedColor(context, R.b.colorAccent)
                    )
                    builder.k = object: b.k.a.a.f { // color picker listener i guess
                        override fun onColorReset(i: Int) { }

                        override fun onColorSelected(i: Int, i2: Int) {
                            editText.setText("#%06X".format(i2 and 0x00FFFFFF)) // remove alpha component
                        }

                        override fun onDialogDismissed(i: Int) { }
                    }
                    builder.show(parentFragmentManager, "COLOR_PICKER")
                }
                setClickable(true)
            }
            setMarginEnd(p)
        }

        val imageInput = TextInput(context, "Image Url").apply { 
            editText.apply {
                inputType = (EditorInfo.TYPE_TEXT_VARIATION_URI or EditorInfo.TYPE_CLASS_TEXT)
                imeOptions = EditorInfo.IME_ACTION_NEXT
            }
            setMarginEnd(p)
        }

        // Pre-fetch webhooks to check permissions
        Utils.threadPool.execute {
            try {
                val perms = StoreStream.getPermissions()
                hasWebhookPermissions = PermissionUtils.can(Permission.MANAGE_WEBHOOKS, perms.permissionsByChannel.get(channelId))
                
                if (hasWebhookPermissions) {
                    allWebhooks = getWebhooks()
                    if (allWebhooks.isNotEmpty()) {
                        defaultWebhook = allWebhooks.firstOrNull()
                    }
                }
            } catch (e: Throwable) {
                logger.error(e)
            }
        }

        val modeInput = Button(context).apply { 
            text = if (modeOverride == null) "directwebhook" else modeOverride
            setBackgroundColor(ColorCompat.getThemedColor(view.getContext(), R.b.colorBackgroundTertiary))
            setOnClickListener {
                Utils.threadPool.execute({
                    val webhooks = HashMap<String, Webhook>()
                    val perms = StoreStream.getPermissions()
                    if (PermissionUtils.can(Permission.MANAGE_WEBHOOKS, perms.permissionsByChannel.get(channelId))) {
                        for(hook in getWebhooks()) {
                            if (hook.token == null) continue
                            var name = hook.name
                            if (name == null) {
                                name = hook.token
                            }
                            while (webhooks.containsKey(name)) {
                                name += "."
                            }
                            webhooks.put(name, hook)
                        }
                    }
                    
                    val modes = plugin.modes.toMutableList()

                    webhooks.keys.forEach {
                        modes.add("Webhook: %s".format(it))
                    }
                    
                    val modeSelector = ModeSelector(modes, {mode -> 
                        if (mode.startsWith("Webhook: ")) {
                            val hook = webhooks.get(mode.drop(9))
                            this.setText("webhooks/%s/%s".format(hook?.id, hook?.token))
                        } else {
                            this.setText(mode)
                        }
                    })
                    modeSelector.show(parentFragmentManager, "Embed Mode")
                    
                })

            }
            setMarginEnd(p)
        }

        val sendBtn = Button(context).apply { 
            text = context.getString(R.h.send_message)
            setOnClickListener {
                try {
                    Utils.threadPool.execute(object : Runnable {
                        override fun run() {
                            onSend(
                                modeOverride ?: modeInput.text.toString(), 
                                authorInput.editText.text.toString(), 
                                titleInput.editText.text.toString(), 
                                contentInput.editText.text.toString(), 
                                urlInput.editText.text.toString(), 
                                imageInput.editText.text.toString(), 
                                colorInput.editText.text.toString()
                            )
                        }
                    })
                } catch (e: Throwable) {
                    Utils.showToast("An error occurred: ${e.message}")
                    logger.error(e)
                }
                dismiss()
            }
        }

        addView(authorInput)
        addView(titleInput)
        addView(contentInput)
        addView(urlInput)
        addView(imageInput)
        addView(colorInput)
        if (modeOverride == null)
            addView(modeInput)
        addView(sendBtn)
    }

    private fun getWebhooks(): Array<Webhook> {
        try {
            return Http.Request.newDiscordRequest("/channels/%d/webhooks".format(channelId))
                .execute()
                .json(Array<Webhook>::class.java)
        } catch (e: Throwable) {
            logger.error(e)
        }
        return emptyArray()
    }

    private fun createWebhook(name: String): Webhook? {
        try {
            val jsonBody = "{\"name\":\"${name}\"}"
            val response = Http.Request.newDiscordRequest("/channels/%d/webhooks".format(channelId), "POST")
                .setRequestBody(jsonBody)
                .execute()
                .json(Webhook::class.java)
            return response
        } catch (e: Throwable) {
            logger.error(e)
            Utils.showToast("Failed to create webhook: ${e.message}")
        }
        return null
    }

    private fun sendWebhookEmbed(webhook: String, author: String, title: String, content: String, url: String, imageUrl: String, color: Int)  {
        try {
            Http.Request("https://discord.com/api/%s".format(webhook), "POST")
                .setHeader("Content-Type", "application/json")
                .executeWithJson(WebhookMessage(
                    null, 
                    listOf(
                        Embed(
                            Author(author),
                            title, 
                            content,
                            url,
                            if (imageUrl.isEmpty()) null else EmbedImage(imageUrl),
                            color
                        )
                    )
                ))
            Utils.showToast("Webhook embed sent successfully")
        } catch (e: Throwable) {
            Utils.showToast("Error sending webhook: ${e.message}")
            logger.error(e)
        }
    }

    private fun sendDirectWebhookEmbed(author: String, title: String, content: String, url: String, imageUrl: String, color: Int)  {
        try {
            // Check if we have webhook permissions
            if (!hasWebhookPermissions) {
                Utils.showToast("You need webhook permissions to use direct webhook")
                // Fall back to sendMessageEmbed
                sendMessageEmbed(author, title, content, url, imageUrl, color)
                return
            }

            // Get or create a webhook
            var webhook = defaultWebhook
            if (webhook == null || webhook.token == null) {
                webhook = createWebhook("SendEmbeds")
            }

            if (webhook == null || webhook.token == null) {
                Utils.showToast("Failed to create or use webhook")
                // Fall back to sendMessageEmbed
                sendMessageEmbed(author, title, content, url, imageUrl, color)
                return
            }

            // Send the webhook
            Http.Request(webhook.url, "POST")
                .setHeader("Content-Type", "application/json")
                .executeWithJson(WebhookMessage(
                    null, 
                    listOf(
                        Embed(
                            Author(author),
                            title, 
                            content,
                            url,
                            if (imageUrl.isEmpty()) null else EmbedImage(imageUrl),
                            color
                        )
                    )
                ))
            Utils.showToast("Embed sent successfully")
        } catch (e: Throwable) {
            Utils.showToast("Error sending webhook: ${e.message}")
            logger.error(e)
            // Fall back to sendMessageEmbed
            sendMessageEmbed(author, title, content, url, imageUrl, color)
        }
    }

    private fun sendMessageEmbed(author: String, title: String, content: String, url: String, imageUrl: String, color: Int) {
        try {
            // Create a JSON-formatted message with embed details
            val embedJson = """
            {
                "embed": {
                    "author": { "name": "${author.replace("\"", "\\\"")}" },
                    "title": "${title.replace("\"", "\\\"")}",
                    "description": "${content.replace("\"", "\\\"").replace("\n", "\\n")}",
                    "url": "${url.replace("\"", "\\\"")}",
                    "color": ${color},
                    ${if (imageUrl.isNotEmpty()) "\"image\": { \"url\": \"${imageUrl.replace("\"", "\\\"")}\" }" else ""}
                }
            }
            """.trimIndent()
            
            // Send as a normal message
            val message = RestAPIParams.Message(
                embedJson,
                NonceGenerator.computeNonce(ClockFactory.get()).toString(),
                null,
                null,
                emptyList(),
                null,
                RestAPIParams.Message.AllowedMentions(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    false
                ),
                null,
                null
            )
            RestAPI.api.sendMessage(channelId, message).subscribe(createActionSubscriber({ 
                Utils.showToast("Embed message sent!")
            }))
        } catch (e: Throwable) {
            Utils.showToast("Error sending message: ${e.message}")
            logger.error(e)
        }
    }

    private fun sendNonBotEmbed(site: String, author: String, title: String, content: String, url: String, imageUrl: String, color: Int) {
        try {
            // Different processing for different sites
            val msg = when (site) {
                "discohook.org" -> {
                    val jsonData = """
                    {
                      "content": null,
                      "embeds": [{
                        "title": "${title.replace("\"", "\\\"")}",
                        "description": "${content.replace("\"", "\\\"").replace("\n", "\\n")}",
                        "color": ${color},
                        "author": { "name": "${author.replace("\"", "\\\"")}" },
                        "url": "${url.replace("\"", "\\\"")}",
                        ${if (imageUrl.isNotEmpty()) "\"image\": { \"url\": \"${imageUrl.replace("\"", "\\\"")}\" }" else ""}
                      }]
                    }
                    """.trimIndent()
                    
                    "https://discohook.org/?data=" + URLEncoder.encode(jsonData, "UTF-8")
                }
                "webhook.lewisakura.moe" -> {
                    "https://webhook.lewisakura.moe/api/webhook?author=${URLEncoder.encode(author, "UTF-8")}&authorIcon=&title=${URLEncoder.encode(title, "UTF-8")}&description=${URLEncoder.encode(content, "UTF-8")}&color=${color.toString(16)}&url=${URLEncoder.encode(url, "UTF-8")}&imageUrl=${URLEncoder.encode(imageUrl, "UTF-8")}"
                }
                else -> {
                    // Default format for other services (rauf embed, etc.)
                    "https://${site}/?author=${URLEncoder.encode(author, "UTF-8")}&title=${URLEncoder.encode(title, "UTF-8")}&description=${URLEncoder.encode(content, "UTF-8")}&color=${color.toString(16)}${if (imageUrl.isNotEmpty()) "&image=${URLEncoder.encode(imageUrl, "UTF-8")}" else ""}${if (url.isNotEmpty()) "&redirect=${URLEncoder.encode(url, "UTF-8")}" else ""}"
                }
            }
            
            val finalMsg = if (plugin.settings.getBool("SendEmbeds_NQNCompatibility", true)) 
                "[](${msg})"
            else
                msg
                
            val message = RestAPIParams.Message(
                finalMsg,
                NonceGenerator.computeNonce(ClockFactory.get()).toString(),
                null,
                null,
                emptyList(),
                null,
                RestAPIParams.Message.AllowedMentions(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    false
                ),
                null,
                null
            )
            RestAPI.api.sendMessage(channelId, message).subscribe(createActionSubscriber({ 
                Utils.showToast("Embed sent successfully!")
            }))
        } catch (e: Throwable) {
            Utils.showToast("Error sending embed: ${e.message}")
            logger.error(e)
        }
    }

    private fun onSend(mode: String, author: String, title: String, content: String, url: String, imageUrl: String, color: String) {
        try {
            if (plugin.extraFunctions.containsKey(mode)) {
                plugin.extraFunctions.get(mode)?.invoke(
                    channelId,
                    author, 
                    title, 
                    content, 
                    url, 
                    imageUrl,
                    color
                )
            }
            else if (mode.startsWith("webhooks/")) {
                sendWebhookEmbed(
                    mode,
                    author, 
                    title, 
                    content, 
                    url, 
                    imageUrl,
                    toColorInt(color)
                )
            }
            else if (mode == "directwebhook") {
                sendDirectWebhookEmbed(
                    author,
                    title,
                    content,
                    url,
                    imageUrl,
                    toColorInt(color)
                )
            }
            else {
                sendNonBotEmbed(
                    mode,
                    author, 
                    title, 
                    content, 
                    url, 
                    imageUrl,
                    toColorInt(color)
                )
            }
        } catch (e: Throwable) {
            Utils.showToast("Error: ${e.message}")
            logger.error(e)
        }
    }

    public fun toColorInt(a: String): Int {
        try {
            return a
                .replace("#", "")
                .toInt(16)
        } catch(e:Throwable) {
            Utils.showToast("Color parser error: %s".format(e.message))
            e.printStackTrace()
        }
        return 0
    }
}

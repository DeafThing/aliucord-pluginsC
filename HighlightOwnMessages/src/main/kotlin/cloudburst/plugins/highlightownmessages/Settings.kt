package cloudburst.plugins.highlightownmessages

import com.aliucord.fragments.SettingsPage
import android.view.View
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.discord.views.CheckedSetting
import com.aliucord.widgets.LinearLayout
import com.aliucord.api.SettingsAPI
import android.widget.TextView
import com.lytefast.flexinput.R
import com.aliucord.views.TextInput
import android.text.InputType
import android.graphics.Color
import android.widget.Button

class Settings(private val settings: SettingsAPI) : SettingsPage() {
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("HighlightOwnMessages")
        val p = DimenUtils.defaultPadding / 2

        addView(Utils.createCheckedSetting(view.context, CheckedSetting.ViewType.SWITCH, "Right and Left", "Whether to make your messages show on the right.").apply {
            val key = "RightLeft"
            isChecked = settings.getBool(key, true)
            setOnCheckedListener {
                settings.setBool(key, it)
            }
        })

        addView(Utils.createCheckedSetting(view.context, CheckedSetting.ViewType.SWITCH, "Left Align Multiline", "Whether to make multiline text left aligned, but add padding.").apply {
            val key = "Multiline"
            isChecked = settings.getBool(key, false)
            setOnCheckedListener {
                settings.setBool(key, it)
            }
        })

        val padding = TextInput(view.context).apply {
            setHint("Padding")
            editText.setText(settings.getInt("Padding", 256).toString())
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.maxLines = 1
            setPadding(p, p, p, p)
        }
        addView(padding)

        addView(TextView(view.context, null, 0, R.i.UiKit_TextView).apply { 
            text = "Set the transparency to 100% to reset."
            setPadding(p, p, p, p)
        })

        val selfFgButton = Button(view.context).apply {
            text = "Select Self Foreground Color"
            setOnClickListener {
                colorPicker("SelfFg")
            }
        }
        addView(selfFgButton)

        val selfBgButton = Button(view.context).apply {
            text = "Select Self Background Color"
            setOnClickListener {
                colorPicker("SelfBg")
            }
        }
        addView(selfBgButton)

        setOnBackPressed {
            try {
                settings.setInt("Padding", padding.editText.text.toString().toInt())
            } catch(e: Throwable) {
                Utils.showToast(e.message.toString())
            }
            return@setOnBackPressed false
        }
    }

    private fun colorPicker(key: String) {
        val initialColor = settings.getInt(key, Color.BLACK)
        val builder = ColorPickerUtils.INSTANCE.buildColorPickerDialog(
            context, 
            Utils.getResId("color_picker_title", "string"), 
            initialColor 
        )
        builder.arguments?.putBoolean("alpha", true)
        builder.k = object: b.k.a.a.f { // color picker listener
            override fun onColorReset(i: Int) { }

            override fun onColorSelected(i: Int, i2: Int) {
                try {
                    settings.setInt(key, i2)
                    Utils.showToast("Color selected: $i2")
                } catch(e: Throwable) {
                    Utils.showToast(e.message.toString())
                }
            }

            override fun onDialogDismissed(i: Int) { }
        }
        builder.show(parentFragmentManager, "COLOR_PICKER")
    }
}

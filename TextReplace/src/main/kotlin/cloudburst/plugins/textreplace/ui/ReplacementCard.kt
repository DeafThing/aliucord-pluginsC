package cloudburst.plugins.textreplace.ui

import android.content.Context
import android.view.View
import android.view.Gravity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.aliucord.views.TextInput
import com.aliucord.views.Button
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.ReflectUtils
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import cloudburst.plugins.textreplace.utils.TextReplacement
import com.discord.views.CheckedSetting
import com.aliucord.Utils
import android.widget.LinearLayout

class ReplacerCard(ctx: Context) : MaterialCardView(ctx) {
    private var onDeleteClick: (() -> Unit)? = null
    val fromInput: TextInput 
    val replacementInput: TextInput 
    val isRegex: CheckedSetting 
    val ignoreCase: CheckedSetting 
    val matchUnsent: CheckedSetting 
    val matchSent: CheckedSetting 
    val matchEmbeds: CheckedSetting 

    init {
        radius = DimenUtils.defaultCardRadius.toFloat()
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundTertiary))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val p = DimenUtils.defaultPadding
        val linearLayout = com.aliucord.widgets.LinearLayout(ctx).apply { 
            setPadding(p, p, p, p)
        }

        fromInput = TextInput(ctx)
        fromInput.setInputHint("Find text...")
        linearLayout.addView(fromInput)

        replacementInput = TextInput(ctx)
        replacementInput.setInputHint("Replace with...")
        linearLayout.addView(replacementInput)

        isRegex = Utils.createCheckedSetting(
            ctx,
            CheckedSetting.ViewType.SWITCH,
            "Regex",
            "Whether to interpret from as regex"
        )
        linearLayout.addView(isRegex)
        
        ignoreCase = Utils.createCheckedSetting(
            ctx,
            CheckedSetting.ViewType.SWITCH,
            "Case Insensitive",
            "Whether to ignore if upper or lower case"
        )
        linearLayout.addView(ignoreCase)

        matchUnsent = Utils.createCheckedSetting(
            ctx,
            CheckedSetting.ViewType.SWITCH,
            "Match unsent",
            "Whether to match unsent messages (user's own messages before they are sent)"
        )
        linearLayout.addView(matchUnsent)

        matchSent = Utils.createCheckedSetting(
            ctx,
            CheckedSetting.ViewType.SWITCH,
            "Match sent",
            "Whether to match sent messages (messages from other users)"
        )
        linearLayout.addView(matchSent)

        matchEmbeds = Utils.createCheckedSetting(
            ctx,
            CheckedSetting.ViewType.SWITCH,
            "Match embeds",
            "Whether to match embeds"
        )
        linearLayout.addView(matchEmbeds)

        // Add delete button at the bottom with full width and padding
        val deleteButton = Button(ctx).apply {
            text = "Delete"
            setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorStatusDanger))
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorTextNormal))
            val smallPadding = DimenUtils.defaultPadding / 2
            setPadding(smallPadding, smallPadding, smallPadding, smallPadding)
            setOnClickListener { onDeleteClick?.invoke() }
            
            // Set layout params to match parent width with some margin
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Add some margin on the sides for better appearance
                val margin = DimenUtils.defaultPadding / 2
                setMargins(margin, DimenUtils.defaultPadding, margin, 0)
            }
        }
        
        linearLayout.addView(deleteButton)

        addView(linearLayout)
    }

    fun setDeleteCallback(callback: () -> Unit) {
        onDeleteClick = callback
    }

    fun TextInput.setInputHint(hint: CharSequence) {
        if (this is CardView) {
            val root = ReflectUtils.invokeMethod(this, "getRoot") as TextInputLayout
            root.hint = hint
        } else {
            (this as TextInputLayout).hint = hint
        }
    }

    public fun apply(replacement: TextReplacement) {
        fromInput.editText.setText(replacement.fromInput)
        replacementInput.editText.setText(replacement.replacement)
        isRegex.isChecked = replacement.isRegex
        ignoreCase.isChecked = replacement.ignoreCase
        matchUnsent.isChecked = replacement.matchUnsent
        matchSent.isChecked = replacement.matchSent
        matchEmbeds.isChecked = replacement.matchEmbeds
    }

    public fun createReplacement(): TextReplacement? {
        if ((fromInput.editText.text.toString() == "") or (replacementInput.editText.text.toString() == "")) return null
        try {
            return TextReplacement(
                fromInput.editText.text.toString(),
                replacementInput.editText.text.toString(),
                isRegex.isChecked,
                ignoreCase.isChecked,
                matchUnsent.isChecked,
                matchSent.isChecked,
                matchEmbeds.isChecked,
            )
        } catch (e: Throwable) {
            
        }
        return null
    }
}
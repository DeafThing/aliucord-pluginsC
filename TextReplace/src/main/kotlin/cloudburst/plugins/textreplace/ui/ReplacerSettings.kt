package cloudburst.plugins.textreplace.ui

import com.aliucord.fragments.SettingsPage
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import cloudburst.plugins.textreplace.TextReplace
import cloudburst.plugins.textreplace.utils.TextReplacement
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import com.aliucord.utils.DimenUtils
import com.aliucord.utils.GsonUtils
import com.aliucord.Utils
import android.graphics.Color
import com.aliucord.views.Button
import com.aliucord.views.ToolbarButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.aliucord.fragments.InputDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.TextView

class ReplacerSettings : SettingsPage() {
    val headerId = View.generateViewId()
    var replacementRules = TextReplace.replacementRules.toMutableList()

    override fun onViewBound(view: View) {
        super.onViewBound(view)
        
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        val ctx = view.context
        setActionBarTitle("TextReplace")
        
        val recyclerAdapter = ReplacerAdapter(this@ReplacerSettings, replacementRules)
        val recycler = RecyclerView(ctx).apply {
            adapter = recyclerAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

            val decoration = DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
            ShapeDrawable(RectShape()).run {
                setTint(Color.TRANSPARENT)
                intrinsicHeight = DimenUtils.defaultPadding
                decoration.setDrawable(this)
            }
            addItemDecoration(decoration)
        }
        addView(recycler)

        Button(ctx).run {
            text = "New Rule"
            DimenUtils.defaultPadding.let {
                setPadding(it, it, it, it)
            }
            setOnClickListener {
                replacementRules.add(TextReplacement.emptyRule())
                recyclerAdapter.notifyItemInserted(replacementRules.size-1)
            }
            linearLayout.addView(this)
        }

        setOnBackPressed {
            try {
                updateRules(recycler)
                TextReplace.mSettings.setObject("TextReplace_Rules", replacementRules.toTypedArray())
                TextReplace.replacementRules = replacementRules.toTypedArray()
            } catch (e: Throwable) {
                Utils.showToast(e.toString())
                return@setOnBackPressed true
            }
            return@setOnBackPressed false
        }

        if (headerBar.findViewById<View>(headerId) == null) {

            addHeaderButton("Import", ContextCompat.getDrawable(ctx, Utils.getResId("ic_file_upload_24dp", "drawable"))) {
                showImportDialog(ctx, recyclerAdapter)
                true
            }
            addHeaderButton("Export", ContextCompat.getDrawable(ctx, Utils.getResId("ic_file_download_white_24dp", "drawable"))) {
                updateRules(recycler)
                Utils.setClipboard("TextReplaceRules", GsonUtils.toJson(replacementRules))
                Utils.showToast("Rules copied to clipboard")
                true
            }

        }
    }

    private fun showImportDialog(ctx: android.content.Context, recyclerAdapter: ReplacerAdapter) {
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val padding = DimenUtils.defaultPadding
            setPadding(padding, padding, padding, padding)
        }

        val titleText = TextView(ctx).apply {
            text = "Import Rules"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, DimenUtils.defaultPadding)
        }
        container.addView(titleText)

        val descText = TextView(ctx).apply {
            text = "Paste previously exported rules here:"
            setPadding(0, 0, 0, DimenUtils.defaultPadding / 2)
        }
        container.addView(descText)

        val editText = EditText(ctx).apply {
            hint = "Rules JSON"
            minLines = 8
            maxLines = 15
            isVerticalScrollBarEnabled = true
            setHorizontallyScrolling(false)
            // Remove any character limits
            filters = arrayOf()
        }

        val scrollView = ScrollView(ctx).apply {
            addView(editText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                DimenUtils.dpToPx(200)
            )
        }
        container.addView(scrollView)

        AlertDialog.Builder(ctx)
            .setView(container)
            .setPositiveButton("Import") { _, _ ->
                try {
                    val inputText = editText.text.toString().trim()
                    if (inputText.isNotEmpty()) {
                        replacementRules.clear()
                        replacementRules.addAll(GsonUtils
                            .fromJson(inputText, Array<TextReplacement>::class.java)
                            .toList())
                        
                        // Notify the adapter that the data has changed
                        recyclerAdapter.notifyDataSetChanged()
                        Utils.showToast("Rules imported successfully")
                    }
                } catch (e: Throwable) {
                    Utils.showToast("Error importing rules: ${e.message}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRules(recycler: RecyclerView) {
        replacementRules.clear()
        var i = 0;
        while (true) {
            val holder = recycler.findViewHolderForLayoutPosition(i) as ReplacerHolder?
            if (holder == null) break
            val replacement = holder.card.createReplacement()
            if (replacement != null) replacementRules.add(replacement)
            i++
        }
    }

}
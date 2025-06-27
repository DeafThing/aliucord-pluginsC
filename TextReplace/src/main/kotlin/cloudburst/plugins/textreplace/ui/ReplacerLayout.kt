package cloudburst.plugins.textreplace.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.fragments.ConfirmDialog
import com.aliucord.fragments.SettingsPage
import cloudburst.plugins.textreplace.utils.TextReplacement
import android.text.TextWatcher
import android.text.Editable

class ReplacerAdapter(
    private val settings: SettingsPage, 
    private val rules: MutableList<TextReplacement>
) : RecyclerView.Adapter<ReplacerHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplacerHolder {
        return ReplacerHolder(this, ReplacerCard(parent.context))
    }
    
    override fun onBindViewHolder(holder: ReplacerHolder, position: Int) {
        val rule = rules[position]
        holder.card.apply(rule)
        
        // Set up delete callback for this position
        holder.card.setDeleteCallback {
            deleteRule(position)
        }
    }

    override fun getItemCount() = rules.size
    
    fun deleteRule(position: Int) {
        if (position >= 0 && position < rules.size) {
            rules.removeAt(position)
            notifyItemRemoved(position)
            // Notify range changed to update positions for remaining items
            notifyItemRangeChanged(position, rules.size - position)
        }
    }
}

class ReplacerHolder(private val adapter: ReplacerAdapter, val card: ReplacerCard) : RecyclerView.ViewHolder(card)
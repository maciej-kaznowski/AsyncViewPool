package com.github.maciejkaznowski.sample

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.maciejkaznowski.library.AsyncViewPool
import com.github.maciejkaznowski.sample.Adapter.Layout.LAYOUT_RES
import kotlinx.android.synthetic.main.activity_recycler_view.*
import kotlinx.android.synthetic.main.view_main_activity_list_item.view.*

class RecyclerViewActivity : AppCompatActivity() {

    object Extras {
        const val WITH_VIEW_POOL =
            "com.github.maciejkaznowski.sample.RecyclerViewActivity.WITH_VIEW_POOL"
    }

    private lateinit var adapter: Adapter
    private var viewPool: AsyncViewPool? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)

        //clear the layout cache so that we can compare between using the AsyncViewPool and not using it
        resources.flushLayoutCache()

        //if requested, create an AsyncViewPool and inflate the layouts
        val withViewPool = intent.extras?.getBoolean(Extras.WITH_VIEW_POOL) ?: false
        viewPool = if (withViewPool) {
            AsyncViewPool(this, recyclerView, true).apply {
                inflate(
                    LAYOUT_RES,
                    10
                ) //start inflating (asynchronously) 10 of our adapter layouts ASAP
            }
        } else {
            null
        }

        //setup RecyclerView
        adapter = Adapter(viewPool)
        with(recyclerView) {
            layoutManager = GridLayoutManager(this@RecyclerViewActivity, 1)
            adapter = this@RecyclerViewActivity.adapter
        }

        //imitate a network call to show items in the adapter at the end
        val delayMs = 3_000L
        Handler().postDelayed({
            adapter.itemCount = 100
        }, delayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        //very important that we destroy the ViewPool once we no longer need it. Failing to do so will result in a memory leak
        viewPool?.destroy()
    }
}


private class Adapter(private val viewPool: AsyncViewPool?) :
    RecyclerView.Adapter<Adapter.ViewHolder>() {

    internal var itemCount: Int = 0
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    object Layout {
        const val LAYOUT_RES = R.layout.view_main_activity_list_item
    }

    override fun getItemCount() = itemCount

    @SuppressLint("SetTextI18n")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //check if our view pool has a view for us, if not - inflate one
        val startInflateTimeNs = System.nanoTime()
        val inflateResult = inflate(parent)
        val inflateTimeMs = (System.nanoTime() - startInflateTimeNs) / 1_000_000L

        //update text and text colour with the inflation result
        if (inflateResult.fromViewPool) {
            inflateResult.view.slowTextView.setTextColor(Color.GREEN)
            inflateResult.view.slowTextView.text =
                "View inflated asynchronously, retrieval took ${inflateTimeMs}ms"
        } else {
            inflateResult.view.slowTextView.setTextColor(Color.RED)
            inflateResult.view.slowTextView.text =
                "View inflated synchronously, inflation took ${inflateTimeMs}ms"
        }

        return ViewHolder(inflateResult.view)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun inflate(parent: ViewGroup): InflateResult {
        val viewFromPool = viewPool?.getView(LAYOUT_RES)
        if (viewFromPool != null) return InflateResult(fromViewPool = true, view = viewFromPool)
        return InflateResult(fromViewPool = false, view = parent inflate LAYOUT_RES)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

data class InflateResult(val fromViewPool: Boolean, val view: View)

private infix fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(this.context).inflate(layoutRes, this, false)
}

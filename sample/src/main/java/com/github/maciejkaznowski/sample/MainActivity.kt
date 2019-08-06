package com.github.maciejkaznowski.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnWith.setOnClickListener { startRecyclerViewActivity(withViewPool = true) }
        btnWithout.setOnClickListener { startRecyclerViewActivity(withViewPool = false) }
    }

    private fun startRecyclerViewActivity(withViewPool: Boolean) {
        Intent(this, RecyclerViewActivity::class.java)
            .apply { putExtra(RecyclerViewActivity.Extras.WITH_VIEW_POOL, withViewPool) }
            .let(this::startActivity)
    }
}

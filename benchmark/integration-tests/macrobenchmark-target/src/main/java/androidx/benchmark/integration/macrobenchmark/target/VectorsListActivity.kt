/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.benchmark.integration.macrobenchmark.target

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VectorsListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "RecyclerView vectors list"
        setContentView(R.layout.activity_vectors_list)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val adapter = VectorEntryAdapter(entries())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun entries() = List(1000) { VectorEntry("ViewXYZ$it") }
}

private data class VectorEntry(val contents: String)

private class VectorEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val content: TextView = itemView.findViewById(R.id.content)
    val subContent: TextView = itemView.findViewById(R.id.subContent)
}

private class VectorEntryAdapter(private val entries: List<VectorEntry>) :
    RecyclerView.Adapter<VectorEntryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VectorEntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.vectors_row, parent, false)
        return VectorEntryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VectorEntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.content.text = entry.contents
        holder.subContent.text = entry.contents
    }

    override fun getItemCount(): Int = entries.size
}

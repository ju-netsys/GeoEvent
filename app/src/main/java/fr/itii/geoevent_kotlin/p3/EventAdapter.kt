package fr.itii.geoevent_kotlin.p3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.databinding.ItemEventBinding
import fr.itii.geoevent_kotlin.p1.Event

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    private val events = mutableListOf<Event>()

    fun submitList(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    inner class ViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvTitle.text = event.title
            binding.tvDescription.text = event.description.ifEmpty {
                binding.root.context.getString(R.string.no_description)
            }
            if (event.authorEmail.isNotEmpty()) {
                binding.tvAuthor.text = "${binding.root.context.getString(R.string.author)} : ${event.authorEmail}"
                binding.tvAuthor.visibility = View.VISIBLE
            } else {
                binding.tvAuthor.visibility = View.GONE
            }
            binding.tvCoordinates.text = String.format("%.5f, %.5f", event.latitude, event.longitude)
            binding.root.setOnClickListener { onEventClick(event) }
        }
    }
}

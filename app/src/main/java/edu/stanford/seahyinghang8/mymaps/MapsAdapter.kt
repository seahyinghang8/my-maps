package edu.stanford.seahyinghang8.mymaps

import android.content.Context
import android.nfc.Tag
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import edu.stanford.seahyinghang8.mymaps.models.UserMap

private const val TAG = "MapsAdapter"

class MapsAdapter(val context: Context, val userMaps: List<UserMap>, val onClickListener: OnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface OnClickListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_map, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val userMap = userMaps[position];
        val textViewTitle = holder.itemView.findViewById<TextView>(R.id.tvMapTitle)
        val textViewDescription = holder.itemView.findViewById<TextView>(R.id.tvMapDescription)
        val textViewNumPlaces = holder.itemView.findViewById<TextView>(R.id.tvMapNumPlaces)

        textViewTitle.text = userMap.title
        textViewDescription.text = userMap.description
        textViewNumPlaces.text = "${userMap.places.size} places"

        holder.itemView.setOnClickListener {
            onClickListener.onItemClick(position)
        }
        holder.itemView.setOnLongClickListener {
            onClickListener.onItemLongClick(position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return userMaps.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}

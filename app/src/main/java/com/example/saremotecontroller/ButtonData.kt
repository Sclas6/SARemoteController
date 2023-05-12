package com.example.saremotecontroller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

data class ButtonData(
    val name: String,
    val user: String,
    val num:Int
)

class ButtonAdapter(private val buttonList: List<ButtonData>, private val listener: OnButtonClickListener) :
    RecyclerView.Adapter<ButtonAdapter.ButtonViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.button_layout, parent, false)
        return ButtonViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val buttonData = buttonList[position]
        holder.button.text = buttonData.name
        if(buttonData.num>=2){
            holder.button.isEnabled=false
        }
        holder.button.setOnClickListener {
            //listener.onButtonClick(buttonData.num.toString())
            listener.onButtonClick(String.format("%s %s %d",buttonData.name,buttonData.user,buttonData.num))
        }
    }

    override fun getItemCount(): Int {
        return buttonList.size
    }

    interface OnButtonClickListener {
        fun onButtonClick(name: String)
    }

    inner class ButtonViewHolder(itemView: View, private val listener: OnButtonClickListener) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.button)

        init {
            button.setOnClickListener {
                listener.onButtonClick(button.text.toString())
            }
        }
    }
}
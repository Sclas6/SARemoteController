package com.example.saremotecontroller

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val buttonData = buttonList[position]
        holder.button.text = "${buttonData.name}\n${buttonData.user}"
        holder.button.maxLines=2
        holder.button.width=780
        holder.button.gravity=2
        holder.numText.text="人数\n"+buttonData.num.toString()+"/2"
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
        val numText: TextView = itemView.findViewById(R.id.numText)
        val button: Button = itemView.findViewById(R.id.button)

        init {
            button.setOnClickListener {
                listener.onButtonClick(button.text.toString())
            }
        }
    }
}
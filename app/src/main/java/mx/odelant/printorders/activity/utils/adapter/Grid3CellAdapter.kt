package mx.odelant.printorders.activity.utils.adapter

import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.utils__adapter__grid_3_cell_row.view.*
import mx.odelant.printorders.R

class Grid3CellAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mRowList: ArrayList<Grid3CellRow> = ArrayList()
    private var bool: Boolean = false
    private var check: Boolean = false

    override fun getItemCount(): Int {
        return mRowList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = mRowList[position]
        when (holder) {
            is Grid3CellContentViewHolder -> holder.bindView(element as Grid3CellContent, bool, check)
            is Grid3CellTitleViewHolder -> holder.bindView(element as Grid3CellTitle)
            is Grid3CellHeaderViewHolder -> holder.bindView(element as Grid3CellHeader, bool)
            else -> throw IllegalArgumentException() as Throwable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            Grid3CellRow.TYPE_CONTENT -> {
                val layout = R.layout.utils__adapter__grid_3_cell_row
                return Grid3CellContentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        layout,
                        parent,
                        false
                    )
                )
            }
            Grid3CellRow.TYPE_HEADER -> {
                val layout = R.layout.utils__adapter__grid_3_cell_row
                return Grid3CellHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        layout,
                        parent,
                        false
                    )
                )
            }
            Grid3CellRow.TYPE_TITLE -> {
                val layout = R.layout.utils__adapter__grid_3_cell_row
                return Grid3CellTitleViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        layout,
                        parent,
                        false
                    )
                )
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mRowList[position].getType()
    }

    class Grid3CellTitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(element: Grid3CellTitle) {
            itemView.grid_item_tv_field_2.text = element.title
            itemView.grid_item_tv_field_2.setTypeface(null, Typeface.BOLD)
            itemView.grid_item_tv_field_1.visibility = View.GONE
            itemView.grid_item_tv_field_3.visibility = View.GONE
        }
    }

    class Grid3CellHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(element: Grid3CellHeader, boolean: Boolean) {

            if (element.hideField1) {
                itemView.grid_item_tv_field_1.visibility = View.GONE
            }
            if (element.hideField2) {
                itemView.grid_item_tv_field_2.visibility = View.GONE
            }
            if (element.hideField3) {
                itemView.grid_item_tv_field_3.visibility = View.GONE
            }

            itemView.grid_item_tv_field_1.text = element.label1
            itemView.grid_item_tv_field_2.text = element.label2
            itemView.grid_item_tv_field_3.text = element.label3
            itemView.grid_item_tv_field_1.setTypeface(null, Typeface.BOLD)
            itemView.grid_item_tv_field_2.setTypeface(null, Typeface.BOLD)
            itemView.grid_item_tv_field_3.setTypeface(null, Typeface.BOLD)

            if (boolean)
                itemView.grid_item_chBox_selectDownload.visibility = View.VISIBLE

            itemView.grid_item_chBox_selectDownload.setOnClickListener(element.checkListener)

        }
    }

    class Grid3CellContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(element: Grid3CellContent, boolean: Boolean, check:Boolean) {

            if (element.listener != null) {
                itemView.isClickable = true
                itemView.setOnClickListener(element.listener)
            }

            if (element.hideField1) {
                itemView.grid_item_tv_field_1.visibility = View.GONE
            }
            if (element.hideField2) {
                itemView.grid_item_tv_field_2.visibility = View.GONE
            }
            if (element.hideField3) {
                itemView.grid_item_tv_field_3.visibility = View.GONE
            }

            itemView.grid_item_tv_field_1.text = element.content1
            itemView.grid_item_tv_field_2.text = element.content2
            itemView.grid_item_tv_field_3.text = element.content3

            if (boolean)
                itemView.grid_item_chBox_selectDownload.visibility = View.VISIBLE

            itemView.grid_item_chBox_selectDownload.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                    itemView.grid_item_chBox_selectDownload.setOnClickListener(element.checkListener)
                else
                    itemView.grid_item_chBox_selectDownload.setOnClickListener(element.checkListenerRemove)

            }

            itemView.grid_item_chBox_selectDownload.isChecked = check
        }
    }

    fun setRowList(list: ArrayList<Grid3CellRow>) {
        mRowList.clear()
        mRowList.addAll(list)
        notifyDataSetChanged()
    }

    fun setRowList(list: ArrayList<Grid3CellRow>, boolean: Boolean) {
        bool = boolean
        mRowList.clear()
        mRowList.addAll(list)
        notifyDataSetChanged()
    }

    fun setCheck(boolean: Boolean) {
        check = boolean
        notifyDataSetChanged()
    }
}

interface Grid3CellRow {
    companion object {
        const val TYPE_TITLE = 1
        const val TYPE_HEADER = 2
        const val TYPE_CONTENT = 3
    }

    fun getType(): Int
}

class Grid3CellTitle(val title: String) : Grid3CellRow {

    override fun getType(): Int {
        return Grid3CellRow.TYPE_TITLE
    }
}

class Grid3CellHeader(
    val label1: String,
    val label2: String,
    val label3: String,
    val checkListener: View.OnClickListener?
    ) : Grid3CellRow {

    var hideField1 = false
    var hideField2 = false
    var hideField3 = false


    override fun getType(): Int {
        return Grid3CellRow.TYPE_HEADER
    }
}

interface Callback {
    fun regretData(item : Grid3CellRow)
}
private var callback: Callback? = null

class Grid3CellContent(
    val content1: String,
    val content2: String,
    val content3: String,
    val listener: View.OnClickListener?,
    val checkListener: View.OnClickListener?,
    val checkListenerRemove: View.OnClickListener?
    ) : Grid3CellRow {

    var hideField1 = false
    var hideField2 = false
    var hideField3 = false

    companion object {
        fun empty(): Grid3CellContent {
            return Grid3CellContent("", "", "", null, null, null)
        }
    }

    override fun getType(): Int {
        return Grid3CellRow.TYPE_CONTENT
    }
}

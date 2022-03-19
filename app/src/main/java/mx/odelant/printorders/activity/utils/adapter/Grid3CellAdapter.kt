package mx.odelant.printorders.activity.utils.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mx.odelant.printorders.R
import mx.odelant.printorders.databinding.UtilsAdapterGrid3CellRowBinding

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
                    UtilsAdapterGrid3CellRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                )
            }
            Grid3CellRow.TYPE_HEADER -> {
                val layout = R.layout.utils__adapter__grid_3_cell_row
                return Grid3CellHeaderViewHolder(
                    UtilsAdapterGrid3CellRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                )
            }
            Grid3CellRow.TYPE_TITLE -> {
                val layout = R.layout.utils__adapter__grid_3_cell_row
                return Grid3CellTitleViewHolder(
                    UtilsAdapterGrid3CellRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                )
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mRowList[position].getType()
    }

    class Grid3CellTitleViewHolder(val binding : UtilsAdapterGrid3CellRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindView(element: Grid3CellTitle) {
            binding.gridItemTvField2.text = element.title
            binding.gridItemTvField2.setTypeface(null, Typeface.BOLD)
            binding.gridItemTvField1.visibility = View.GONE
            binding.gridItemTvField3.visibility = View.GONE
        }
    }

    class Grid3CellHeaderViewHolder(val binding: UtilsAdapterGrid3CellRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindView(element: Grid3CellHeader, boolean: Boolean) {

            if (element.hideField1) {
                binding.gridItemTvField1.visibility = View.GONE
            }
            if (element.hideField2) {
                binding.gridItemTvField2.visibility = View.GONE
            }
            if (element.hideField3) {
                binding.gridItemTvField3.visibility = View.GONE
            }

            binding.gridItemTvField1.text = element.label1
            binding.gridItemTvField2.text = element.label2
            binding.gridItemTvField3.text = element.label3
            binding.gridItemTvField1.setTypeface(null, Typeface.BOLD)
            binding.gridItemTvField2.setTypeface(null, Typeface.BOLD)
            binding.gridItemTvField3.setTypeface(null, Typeface.BOLD)

            if (boolean)
                binding.gridItemChBoxSelectDownload.visibility = View.VISIBLE

            binding.gridItemChBoxSelectDownload.setOnClickListener(element.checkListener)

        }
    }

    class Grid3CellContentViewHolder(val binding: UtilsAdapterGrid3CellRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindView(element: Grid3CellContent, boolean: Boolean, check:Boolean) {

            if (element.listener != null) {
                itemView.isClickable = true
                itemView.setOnClickListener(element.listener)
            }

            if (element.hideField1) {
                binding.gridItemTvField1.visibility = View.GONE
            }
            if (element.hideField2) {
                binding.gridItemTvField2.visibility = View.GONE
            }
            if (element.hideField3) {
                binding.gridItemTvField3.visibility = View.GONE
            }

            binding.gridItemTvField1.text = element.content1
            binding.gridItemTvField2.text = element.content2
            binding.gridItemTvField3.text = element.content3

            if (boolean)
                binding.gridItemChBoxSelectDownload.visibility = View.VISIBLE

            binding.gridItemChBoxSelectDownload.setOnCheckedChangeListener { compoundButton, b ->
                if (b)
                    binding.gridItemChBoxSelectDownload.setOnClickListener(element.checkListener)
                else
                    binding.gridItemChBoxSelectDownload.setOnClickListener(element.checkListenerRemove)

            }

            binding.gridItemChBoxSelectDownload.isChecked = check
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

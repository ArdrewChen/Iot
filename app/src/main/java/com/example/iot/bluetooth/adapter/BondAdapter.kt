package com.example.iot.bluetooth.adapter

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.view.View
import android.view.ViewGroup
import com.example.iot.databinding.ItemBondBinding
import com.example.iot.bluetooth.base.BaseBindRecyclerAdapter


class BondAdapter(val activity: Activity,val bondAdapterListener: BonAdapterListener) : BaseBindRecyclerAdapter<ItemBondBinding>() {
    private var list: MutableList<BluetoothDevice> = ArrayList()

    override fun getViewBinding(arent: ViewGroup): ItemBondBinding {
        return ItemBondBinding.inflate(activity.layoutInflater, arent, false)
    }

    fun update(list: MutableList<BluetoothDevice>) {
        this.list = list
        notifyDataSetChanged()
    }


    override fun initView(mBinding: ItemBondBinding, view: View, position: Int) {
        val item = list[position]
        mBinding.name.text = item.name
        mBinding.address.text = item.address

        mBinding.connect.setOnClickListener {
            bondAdapterListener.onConnect(item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface BonAdapterListener{
        fun onConnect(bluetoothDevice: BluetoothDevice)
    }
}
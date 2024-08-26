package com.kunzhut.nfc_tool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible

class MainActivity : ComponentActivity() {

    private var press : Long = 0
    private lateinit var nfsTextoutput : TextView
    private lateinit var nfsinfocheck : TextView
    private lateinit var nfstech : TextView
    private lateinit var nfcAcheckbox : CheckBox
    private lateinit var isodepcheckbox : CheckBox
    private lateinit var mifareclassicCheckbox : CheckBox
    private lateinit var edittextnfcgetCommand : EditText
    private lateinit var nfsinfoabouttagoutput : TextView
    private val stringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        actionBar!!.hide()

        setContentView(R.layout.nfslayout)
        nfsTextoutput = findViewById(R.id.nfstextoutput)
        nfsinfocheck = findViewById(R.id.nfsinfodiscchecker)
        nfstech = findViewById(R.id.nfstechlist)
        nfsTextoutput.movementMethod = ScrollingMovementMethod()
        nfcAcheckbox=findViewById(R.id.checkbox_NfcA)
        isodepcheckbox=findViewById(R.id.checkbox_isodep)
        nfsinfoabouttagoutput = findViewById(R.id.nfsinfooutput)
        nfsinfoabouttagoutput.movementMethod = ScrollingMovementMethod()
        nfsinfocheck.movementMethod = ScrollingMovementMethod()
        nfstech.movementMethod = ScrollingMovementMethod()
        edittextnfcgetCommand = findViewById(R.id.edittextnfcgetCommand)
        mifareclassicCheckbox = findViewById(R.id.checkbox_mifareclassic)
        edittextnfcgetCommand.isVisible = false
        nfsTextoutput.isClickable = true
        nfsinfoabouttagoutput.isClickable =true

        nfcAcheckbox.setOnCheckedChangeListener{ _, isChecked ->

            if(isChecked){
                isodepcheckbox.isChecked = false
                mifareclassicCheckbox.isChecked=false
                edittextnfcgetCommand.isVisible = true
                edittextnfcgetCommand.setText("")
            }else{
                edittextnfcgetCommand.isVisible = false
            }
        }

        isodepcheckbox.setOnCheckedChangeListener {_, isChecked ->

            if(isChecked){
                nfcAcheckbox.isChecked = false
                mifareclassicCheckbox.isChecked = false
                edittextnfcgetCommand.isVisible = true
                edittextnfcgetCommand.setText("")
            }else{
                edittextnfcgetCommand.isVisible = false
            }
        }
        mifareclassicCheckbox.setOnCheckedChangeListener{_, isChecked ->

            if(isChecked){
                nfcAcheckbox.isChecked=false
                isodepcheckbox.isChecked=false
                edittextnfcgetCommand.isVisible = true
                edittextnfcgetCommand.setText("")
            }else{
                edittextnfcgetCommand.isVisible = false
            }
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {

                (applicationContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("copieddata", nfsTextoutput.text))
                Toast.makeText(this@MainActivity, "Successful copy", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })

        nfsTextoutput.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }


    }

    override fun onNewIntent(intent: Intent?) {

        super.onNewIntent(intent!!)
        val action = intent.action

        if (NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {

            stringBuilder.clear()
            if (NfcAdapter.ACTION_TECH_DISCOVERED == action){
                stringBuilder.append("ACTION_TECH\n")
            }
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == action){
                stringBuilder.append("ACTION_NDEF\n")
            }
            if (NfcAdapter.ACTION_TAG_DISCOVERED == action){
                stringBuilder.append("ACTION_TAG\n")
            }

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            stringBuilder.append("Scanned Tag UID: ${byteArrToHex(tag!!.id)}")
            nfsinfocheck.text=(stringBuilder.toString())

            stringBuilder.clear()

            val techarr = tag.techList

            val iter = techarr.iterator()

            while(iter.hasNext()){
                stringBuilder.append("${iter.next()}\n")
            }
            nfstech.text=(stringBuilder.toString())

            Thread(Runnable {
                try {
                    stringBuilder.clear()
                    if(nfcAcheckbox.isChecked){
                        val nfca = NfcA.get(tag)

                        nfsinfoabouttagoutput.text=("Sak: ${nfca.sak}\nAtqa: ${byteArrToHex(nfca.atqa)}\nMaxTransceiveLength: ${nfca.maxTransceiveLength}")

                        if(edittextnfcgetCommand.text.toString() != "") {

                            nfca.connect()
                            nfca.timeout = 5000

                            val output =
                                nfca.transceive(hexStrToArray(edittextnfcgetCommand.text.toString()))

                            stringBuilder.append(byteArrToHex(output))
                        }else{
                            stringBuilder.append("Command not set")
                        }

                    }

                    if(isodepcheckbox.isChecked){

                        val isodep = IsoDep.get(tag)

                        nfsinfoabouttagoutput.text=("LengthApduSupp: ${isodep.isExtendedLengthApduSupported}\nMaxTransceiveLength: ${isodep.maxTransceiveLength}\nHistorical Bytes: ${byteArrToHex(isodep.historicalBytes)}")
                        isodep.connect()
                        isodep.timeout = 5000

                        val responsearr = isodep.transceive(selectApdu("2PAY.SYS.DDF01".toByteArray(Charsets.UTF_8)))

                        stringBuilder.append("2PAY.SYS.DDF01: ${byteArrToHex(responsearr)}\n")

                        if(edittextnfcgetCommand.text.toString() != "") {
                            val responsearr2 = isodep.transceive(
                                selectApdu(
                                    hexStrToArray(edittextnfcgetCommand.text.toString())
                                )
                            )

                            stringBuilder.append("Command output: ${byteArrToHex(responsearr2)}")
                        }else{
                            stringBuilder.append("Command output: Command not set")
                        }
                    }
                    if(mifareclassicCheckbox.isChecked){
                        val mifareClassic = MifareClassic.get(tag)
                        val ndefmifare = Ndef.get(tag)
                        val miftype = when(mifareClassic.type){
                            -1 -> "Unknown"
                            0 -> "Classic"
                            1 -> "Type_Plus"
                            2 -> "Type_Pro"
                            else -> "Error"
                        }

                        stringBuilder.append("Type: ${mifareClassic.type}(${miftype})\nTag Size: ${mifareClassic.size}\nBlock Count: ${mifareClassic.blockCount}\nMaxTransceiveLength: ${mifareClassic.maxTransceiveLength}\nSector Count: ${mifareClassic.sectorCount}\n")

                        if(edittextnfcgetCommand.text.toString() != "") {

                            mifareClassic.connect()
                            mifareClassic.timeout = 5000

                            for(a in 0..<mifareClassic.sectorCount){

                                if(mifareClassic.authenticateSectorWithKeyA(a, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)){
                                    stringBuilder.append("Sector $a success auth with KEY_MIFARE_APPLICATION_DIRECTORY\n")
                                }else if (mifareClassic.authenticateSectorWithKeyA(a, MifareClassic.KEY_DEFAULT)) {
                                    stringBuilder.append("Sector $a success auth with KEY_DEFAULT\n")
                                } else if (mifareClassic.authenticateSectorWithKeyA(a,MifareClassic.KEY_NFC_FORUM)) {
                                    stringBuilder.append("Sector $a success auth with KEY_NFC_FORUM\n")
                                }else{
                                    stringBuilder.append("Sector $a Authorization denied\n")
                                }
                            }

                            nfsinfoabouttagoutput.text=(stringBuilder.toString())
                            stringBuilder.clear()
                            //  val responsearr3 =
                            //     mifareClassic.transceive(hexStrToArray(edittextnfcgetCommand.text.toString()))

                            stringBuilder.append("Command Output: ${byteArrToHex(mifareClassic.readBlock(edittextnfcgetCommand.text.toString().toInt()))}")
                            ndefmifare.connect()
                            stringBuilder.append(byteArrToHex(ndefmifare.ndefMessage.toByteArray()))

                        }else{
                            nfsinfoabouttagoutput.text=(stringBuilder.toString())
                            stringBuilder.clear()
                            stringBuilder.append("Command Output: Command not set and auth info will not be displayed")
                        }
                    }

                    nfsTextoutput.text=(stringBuilder.toString())

                }catch (a : Exception){
                    nfsTextoutput.text=(a.toString())
                }
            }).start()


        }

    }

    private fun byteArrToHex(src: ByteArray): String {
        val strb = StringBuilder()

        if (src.isEmpty()) {
            return "Error"
        }
        val buffer = CharArray(2)

        for (i in src.indices) {
            buffer[0] = Character.forDigit((src[i].toInt() ushr 4) and 0x0F, 16).uppercaseChar()
            buffer[1] = Character.forDigit(src[i].toInt() and 0x0F, 16).uppercaseChar()
            strb.append(buffer)
        }

        return strb.toString()
    }

   private fun selectApdu(data: ByteArray): ByteArray {
        val commandApdu = ByteArray(6 + data.size)
        commandApdu[0] = 0x00.toByte()
        commandApdu[1] = 0xA4.toByte()
        commandApdu[2] = 0x04.toByte()
        commandApdu[3] = 0x00.toByte()
        commandApdu[4] = (data.size and 0x0FF).toByte()
        data.copyInto(commandApdu, 5, 0, data.size)
        commandApdu[commandApdu.size - 1] = 0x00.toByte()
        return commandApdu
    }

   private fun hexStrToArray(hexstrcommand : String) : ByteArray{
        val tempstrarr = hexstrcommand.split(",")
        val tempbytearr = ByteArray(tempstrarr.size)

        tempstrarr.onEachIndexed{index, string ->
            tempbytearr[index] = string.replace("0x", "").toInt(16).toByte()
        }
        return tempbytearr
    }

    override fun onBackPressed() {
        if((System.currentTimeMillis()-press)>2000 || press==0L){
            press=System.currentTimeMillis()
            Toast.makeText(this,"Нажмите \"Назад\" еще раз, чтобы выйти", Toast.LENGTH_SHORT).show()
        }else{
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val conf = Configuration(newBase!!.resources.configuration)
        conf.fontScale=1.0f
        applyOverrideConfiguration(conf)
        super.attachBaseContext(newBase)
    }

}
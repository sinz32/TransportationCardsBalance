package me.sinz.transitcard

import android.nfc.Tag
import android.nfc.tech.NfcF
import me.sinz.library.SinZ
import java.io.ByteArrayOutputStream

class FeliCa(tag: Tag) {

    var id: String? = null
    var balance:Int? = null

    init {
        val nf = NfcF.get(tag)

        nf.connect()
        val data = nf.transceive(createCommand(tag.id, 0, 10))
        val data_str: String = SinZ.bytes2hex(data)

        //응답길이 응답코드 id(제조자코드+카드식별번호) 상태flag1.0 상태flag2.0 블록수 사용내역
        this.id = data_str.substring(4, 4 + 16)
        readLastUsage(data)
        nf.close()
    }

    @Throws(java.lang.Exception::class)
    private fun createCommand(id: ByteArray, offset: Int, count: Int): ByteArray? {
        val bout = ByteArrayOutputStream(128) //일단 넉넉하게 잡음
        bout.write(0x00)  //데이터 길이, 일단 0 넣어두고 다 채운 뒤에 변경 예정
        bout.write(0x06)  //06은 암호화 없이 읽는 것을 의미하는 FeliCa 명령어
        bout.write(id)       //카드를 태그했을 때 읽어온 ID
        bout.write(0x01)  //서비스 코드 길이
        bout.write(0x0f)  // Suica 사용내역 서비스 코드의 low byte (리틀 엔디안 방식)
        bout.write(0x09)  // Suica 사용내역 서비스 코드의 high byte (리틀 엔디안 방식)
        bout.write(count)    //읽을 블록 수
        for (n in offset until offset + count) {
            bout.write(0x80) //블록 엘리먼트 상위 바이트 「FeliCa 사용자 메뉴얼 발췌」의 4.3항 참조
            bout.write(n)       //블록 번호
        }
        val cmd = bout.toByteArray()
        cmd[0] = cmd.size.toByte() //데이터 길이 넣음
        return cmd
    }

    private fun readLastUsage(data: ByteArray) {
        val offset = 13
//        History datum = new History(data, 13 + n * 16);

        /*
         0 - 단말 종류. 개찰구, 발매기 등
         1 - 처리 내역. 충전, 사용 등
         2, 3 - 나는 모르는 값
         4, 5 - 사용 날짜
         6, 7 - 승차역 노선명, 승차역. 버스나 물건 구매라면 다른 값
         8, 9 - 하차역 노선명, 하차역. 버스나 물건 구매라면 다른 값
         10 ~ 11 - 잔액 (리틀 엔디안 방식으로 저장됨)
        */
        balance = toInt(data, offset, 11, 10)
        /*
         12 ~ 14 - 사용 기록 일련번호
         15 - 지역 id
        */

    }

    private fun toInt(res: ByteArray, offset: Int, vararg index: Int): Int {
        var num = 0
        for (i in index) {
            num = num shl 8
            num += res[offset + i].toInt() and 0x0ff
        }
        return num
    }


}
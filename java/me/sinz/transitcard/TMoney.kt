package me.sinz.transitcard

import android.nfc.tech.IsoDep
import me.sinz.library.SinZ

//이름은 티머니지만, 국내 전국호환 교통카드는 대부분 동일
//「금융IC카드 표준 - part2(개방형)」을 참고하며 구현
class TMoney(id: IsoDep) {

    var type: String? = "(알 수 없음)"
    var id: String? = null
    var balance:Int? = null

    init {
        init(id);
    }

    @Throws(Exception::class)
    fun init(id: IsoDep) {
        id.connect()
        val transceive =
            id.transceive(str2bytes("00A40400" + "07" + "A0000004520001" + "00")) //마지막 00은 예상되는 최대 바이트의 수이며, 00으로 적으면 256바이트를 의미

        //data는 레코드. Tag, size, value 순서, value나 size는 없을 수도 있음?
        val data_str = SinZ.bytes2hex(transceive)
        val data = data_str.toCharArray()

        //9000으로 끝나는 경우 = 정상적으로 읽었음을 의미. SW1, SW2가 각각 1바이트씩 뒤에 붙고, 99 00이 정상 처리된 경우를 의미
        if (!data_str.endsWith("9000")) return

        var balanceReadCommand = "905C000004"; //이 값이 표준인데, 다른 명령어를 사용하는 카드도 있음
        var aid: String? = null
        var aidLength: String? = null

        //1바이트 = 2칸씩 읽음, 중간에 n값 수정해서 이미 읽거나 안읽어도 되는 부분 넘김
        var n = 0
        while (n < data.size) {
            //현재 바이트
            val s = data[n].toString() + "" + data[n + 1]

            // FCI Template
            if (s == "6F") {
                n += 2 //용도 모름
            }

            //AID. 어차피 전부 A0000004520001로 동일
            else if (s == "84") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //7
                n += 2 * size + 2
            }

            //FCI Proprietary Template
            else if (s == "A5") {
                n += 2
            }

            //카드 규격
            else if (s == "50") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt() //2
                //선불은 01 00, 후불은 11 00
                n += 2 * size + 2
            }

            //지원 항목 (하이패스 등)
            else if (s == "47") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //2
                n += 2 * size + 2
            }

            //IDCENTER (사업자)
            else if (s == "43") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //1
                //티머니, 캐시비 등 구분
                type = getCardType(data_str.substring(n + 4, n + 4 + 2 * size).toInt(16))
                n += 2 * size + 2
            }

            //잔액 조회 명령어
            else if (s == "11") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //7

                //전부 905C000004로 같아야 하는데, 티머니와 레일플러스는 904C000004가 저장되어 있음
                balanceReadCommand = data_str.substring(n + 4, n + 4 + 2 * size)
                n += 2 * size + 2
            }

            //교통 호환 AFD AID
            else if (s == "4F") {
                //종종 길이나 값이 다른 카드가 있음
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //7 또는 8
                aidLength = data_str.substring(n + 2, n + 2 + 2)
                aid = data_str.substring(n + 4, n + 4 + 2 * size)
                n += 2 * size + 2
            }

            //부가 데이터 파일 정보
            else if (s == "9F" && data[n + 2] == '1' && data[n + 3] == '0') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16) //3
                //항상 EA 00 34이 저장. 용도 모름
                n += 2 * size + 4
            }

            //카드 소지자 정보
            else if (s == "45") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt() //1
                /*
                 01 일반
                 02 어린이
                 03 청소년
                 04 경로
                 05 장애인
                 11 버스
                 12 화물자
                 12 ~ 15 ???
                 */
                n += 2 * size + 2
            }

            //카드 유효기간
            else if (s == "5F" && data[n + 2] == '2' && data[n + 3] == '4') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16) //2
                //yymm 형식으로 저장
                n += 2 * size + 4
            }

            //카드 일련 번호
            else if (s == "12") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //8
                this.id = data_str.substring(n + 4, n + 4 + 2 * size)
                n += 2 * size + 2
            }

            //카드 관리 번호
            else if (s == "13") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //8
                n += 2 * size + 2
            }

            //FCI Issuer Discretionary Data
            else if (s == "BF" && data[n + 2] == '0' && data[n + 3] == 'C') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16) //?
                n += 2 * size + 4
            }
            n += 2
        }

        //잔액조회
        //"CLA INS P1 P2 Lc Data Le"를 전송한 뒤에, balanceReadCommand를 전송하면 읽힘
        //Lc는 AID의 길이를 넣고, Data에는 AID를 넣으면 됨
        //00 A4 04 00 + 07 + D4100000030001 + 00
        id.transceive(str2bytes("00A40400" + aidLength + aid + "00"))

        this.balance = SinZ.bytes2hex(id.transceive(str2bytes(balanceReadCommand)))
            .substring(0, 8).toInt(16)

        id.close()
    }


    private fun getCardType(type: Int): String {
        return when (type) {
//            0x00 -> "Reserved"
            0x01 -> "금융결제원"
            0x02 -> "에이캐시"
            0x03 -> "마이비"
//            0x04 -> "Reserved"
            0x05 -> "브이캐시"
            0x06 -> "몬덱스코리아"
            0x07 -> "한국도로공사"
            0x08 -> "티머니" //한국스마트카드
            0x09 -> "코레일네트웍스"
//            0x0A -> "Reserved"
            0x0B -> "이즐 (캐시비)" //이비. 이후에 캐시비로 통합됨
            0x0C -> "서울특별시버스운송사업조합"
            0x0D -> "카드넷"
            0x21 -> "레일플러스" //표준안에는 없으나, 해당 교통카드로 직접 확인해본 값
            else -> "(알 수 없음)"
        }
    }

    private fun str2bytes(str: String): ByteArray {
        val length = str.length
        val arr = ByteArray(length / 2)
        var n = 0
        while (n < length) {
            arr[n / 2] =
                ((Character.digit(str[n], 16) shl 4) + Character.digit(str[n + 1], 16)).toByte()
            n += 2
        }
        return arr
    }
}
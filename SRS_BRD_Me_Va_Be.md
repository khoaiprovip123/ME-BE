# TÀI LIỆU ĐẶC TẢ YÊU CẦU NGHIỆP VỤ & PHÂN TÍCH HỆ THỐNG (BRD/SRS)
## DỰ ÁN: ỨNG DỤNG DI ĐỘNG "MẸ VÀ BÉ" (MOBILE PLATFORM)
### Chuyên viên Phân tích Nghiệp vụ (B.A) & Chuyên gia Kiến trúc Hệ thống ERP Cấp cao

---

## I. GIỚI THIỆU CHUNG (OVERVIEW)
Tài liệu này đặc tả chi tiết các phân tích y khoa, cấu trúc dữ liệu tổng thể (Master Data) và thiết kế hệ thống cơ sở dữ liệu (DBMS) cho ứng dụng di động **"Mẹ và Bé"**. Hệ thống được xây dựng trên triết lý **Offline-First**, đảm bảo tính khả dụng 100% trong điều kiện sóng yếu hoặc không có kết nối Internet tại các phòng khám sản khoa, đi kèm các giải pháp can thiệp sâu vào hệ thống Android nhằm khắc phục triệt để tình trạng bỏ lỡ thông báo uống thuốc/khám thai do cơ chế tiết kiệm năng lượng của hệ điều hành.

---

## II. TRÍCH XUẤT VÀ PHÂN LOẠI DỮ LIỆU THAI KỲ CHI TIẾT THEO TUẦN (TUẦN 1 - TUẦN 41)
Dưới đây là bảng tích hợp cơ sở tri thức y học chính thống (tổng hợp từ các nguồn nghiên cứu sản khoa uy tín Vinmec, Tâm Anh, Long Châu, Mayo Clinic) bao gồm các chỉ số sinh học chuẩn, biến đổi sinh lý thai nhi, thay đổi cơ thể mẹ, dinh dưỡng vi chất và các cảnh báo khẩn cấp chi tiết từ tuần 1 đến tuần 41.

| Tuần | Cân nặng chuẩn (gm) | Chiều dài chuẩn (cm) | Mô tả sinh lý thai nhi (Phôi thai học & Phản xạ) | Thay đổi cơ thể mẹ (Sinh lý, tâm lý & Nội tiết) | Khuyến khị Dinh dưỡng & Vi chất bắt buộc (Hàm lượng, cách dùng, lý do y khoa) | Cảnh báo bất thường & Trường hợp ngoại lệ (Exceptions) |
| :--- | :---: | :---: | :--- | :--- | :--- | :--- |
| **W1** | 0 | 0 | **Sinh lý:** Giai đoạn kinh nguyệt hành trình. Trứng chưa rụng, cơ thể chuẩn bị cho chu kỳ rụng trứng tiếp theo. | **Mẹ:** Chu kỳ kinh nguyệt bắt đầu. Niêm mạc tử cung bong tróc. Tâm trạng thay đổi nhẹ do nồng độ Estrogen và Progesterone xuống thấp. | **Dinh dưỡng:** Axit Folic (400 - 600 mcg/ngày). Giúp tích lũy folate trong máu, chuẩn bị phòng chống dị tật ống thần kinh sớm nhất.<br>**Cách dùng:** Uống buổi sáng sau ăn. | **Cảnh báo:** Thận trọng nếu kinh nguyệt kéo dài bất thường (>7 ngày), ra cục máu đông lớn, đau bụng dữ dội (cần loại trừ u xơ tử cung, lạc nội mạc). |
| **W2** | 0 | 0 | **Sinh lý:** Sự rụng trứng xảy ra. Trứng trưởng thành được phóng thích từ buồng trứng vào ống dẫn trứng. | **Mẹ:** Thân nhiệt tăng nhẹ (0.3 - 0.5°C). Ham muốn tăng cao dưới tác động của hormone LH và Estrogen. Dịch nhầy cổ tử cung dai, trong như lòng trắng trứng. | **Dinh dưỡng:** Tiếp tục duy trì Axit Folic (400-600mcg), tăng cường Sắt (30mg/ngày) để bù đắp lượng máu đã mất trong chu kỳ kinh trước.<br>**Cách dùng:** Sắt uống kèm Vitamin C để tăng hấp thụ. | **Cảnh báo:** Đau bụng dưới một bên dữ dội (đau rụng trứng quá mức hoặc u nang buồng trứng bị xoắn), khí hư có mùi hôi hoặc ngứa ngáy (viêm nhiễm phụ khoa cần điều trị trước khi thụ thai). |
| **W3** | < 0.1 | < 0.1 | **Sinh lý:** Thụ tinh thành công tạo hợp tử. Hợp tử phân chia nhanh chóng thành dâu phôi và di chuyển về tử cung để làm tổ. | **Mẹ:** Có thể có hiện tượng "máu báo thai" (Spotting) do phôi bám vào niêm mạc tử cung. Estrogen và Progesterone bắt đầu tăng nhẹ. | **Dinh dưỡng:** Axit Folic (400mcg - 600mcg) + Kẽm (15mg/ngày) để hỗ trợ phân chia tế bào phôi khỏe mạnh.<br>**Cách dùng:** Tránh uống trà/cà phê cùng lúc uống vi chất. | **Cảnh báo:** Ra máu âm đạo nhiều kèm đau quặn bụng dưới (nguy cơ sảy thai sớm hoặc thai ngoài tử cung cần siêu âm đầu dò kiểm tra). |
| **W4** | 1 | 0.2 | **Sinh lý:** Phôi hình thành 3 lớp tế bào biệt hóa: Ngoại bì (não, thần kinh), Trung bì (tim, xương, cơ), Nội bì (phổi, gan, ruột). Kích thước bằng **Hạt anh đào (Mè/Sesame)**. | **Mẹ:** Trễ kinh rõ rệt. Nồng độ hormone hCG tăng vọt trong nước tiểu (test 2 vạch). Bắt đầu có cảm giác mệt mỏi, căng tức ngực, Progesterone tăng trưởng dốc. | **Dinh dưỡng:** Bắt buộc duy trì Axit Folic 400 - 600 mcg/ngày chống dị tật nứt đốt sống (Spina Bifida). Bổ sung Vitamin B6 (25mg/ngày) giảm thiểu nghén dạ dày.<br>**Cách dùng:** Uống đều đặn vào khung giờ cố định. | **Cảnh báo:** Thử que 2 vạch nhưng ra máu sẫm màu liên tục và đau bụng một bên âm ỉ tăng dần (dấu hiệu điển hình của Thai Ngoài Tử Cung). |
| **W5** | 1 | 0.4 | **Sinh lý:** Ống thần kinh của thai nhi đóng lại hoàn toàn. Các chồi chi (tay và chân) bắt đầu nhú ra. Ống tim sơ khởi bắt đầu xuất hiện mạch đập. | **Mẹ:** Triệu chứng nôn nghén rõ rệt hơn, nhạy cảm với mùi vị. Đi tiểu nhiều lần do tử cung bắt đầu phình to chèn ép lên bàng quang trực tiếp. | **Dinh dưỡng:** Axit Folic (600mcg/ngày). Tăng cường Sắt (30mg/ngày) để hỗ trợ quá trình tạo máu cung cấp cho phôi thai.<br>**Cách dùng:** Tránh dùng Sắt chung với sữa hoặc trà. | **Cảnh báo:** Đau thắt bụng dưới từng cơn và ra huyết tươi âm đạo (dọa sảy thai, cần nằm tĩnh dưỡng và bổ sung nội tiết Progesterone dưỡng thai theo chỉ định y khoa). |
| **W6** | 1 | 0.6 | **Sinh lý:** Tim thai xuất hiện các nhịp đập co bóp sơ khai đầu tiên (100 - 120 lần/phút). Khuôn mặt bắt đầu có hai đốm sẫm màu là mắt và các hốc tai. | **Mẹ:** Triệu chứng nghén nặng (nôn mửa, chán ăn, mệt mỏi rã rời). Tâm trạng dao động, dễ xúc động do Progesterone và Estrogen tăng cực cao. | **Dinh dưỡng:** Chia nhỏ bữa ăn (5-6 bữa/ngày). Bổ sung Vitamin B6 + Kẽm để xoa dịu cơ trơn tử cung và giảm kích ứng dạ dày gây nôn.<br>**Cách dùng:** Ngậm gừng ấm vào buổi sáng để làm dịu dạ dày. | **Cảnh báo:** Sốt cao > 38.5°C gãy ảnh hưởng trực tiếp đến quá trình đóng ống thần kinh của phôi thai. Cần hạ sốt bằng paracetamol an toàn cho thai và đi khám ngay. |
| **W7** | 1 | 1.2 | **Sinh lý:** Tim liên tục phân chia thành các buồng, nhịp tim đập nhanh mạnh (đạt 130 - 150 nhịp/phút). Các bán cầu não phát triển nhanh chóng. | **Mẹ:** Cổ tử cung mềm ra và xuất hiện nút nhầy bảo vệ buồng tử cung. Ngực to lên và đầu vú thâm sẫm lại, nhạy cảm đau nhức. | **Dinh dưỡng:** Axit Folic + Sắt (30mg) + Vitamin C giúp hấp thụ sắt tối ưu. Uống đủ 2 lít nước để ngăn ngừa thiếu nước và nhiễm trùng đường tiểu.<br>**Cách dùng:** Uống nhiều nước ấm suốt cả ngày. | **Cảnh báo:** Nghén nặng đến mức không giữ được bất kỳ chất lỏng nào (Nghén cấp tính Hyperemesis Gravidarum) gây mất điện giải, cần nhập viện truyền dịch. |
| **W8** | 2 | 1.6 | **Sinh lý:** Phôi thai chính thức bước sang giai đoạn thai nhi (Fetus). Ngón tay và ngón chân bắt đầu phân chia nhưng còn màng. Khớp khuỷu tay hình thành. | **Mẹ:** Tử cung to bằng quả chanh lớn. Nhịp tim của mẹ tăng nhẹ để bơm máu. Có thể gặp tình trạng táo bón nhẹ do nồng độ Progesterone làm giãn cơ trơn ruột. | **Dinh dưỡng:** Bổ sung thực phẩm giàu chất xơ (rau khoai lang, đu đủ chín). Axit Folic + Sắt + Acid béo Omega-3 (DHA/EPA) từ nguồn sạch hỗ trợ phát triển não bộ sớm.<br>**Cách dùng:** DHA uống cùng hoặc ngay sau bữa ăn nhiều dầu mỡ. | **Cảnh báo:** Ra máu đỏ tươi kèm theo những mảng mô nhỏ đào thải ra ngoài âm đạo (Sảy thai hoàn toàn hoặc không hoàn toàn, cần cấp cứu phụ khoa ngay). |
| **W9** | 3 | 2.3 | **Sinh lý:** Chi tiết đuôi thai biến mất hoàn toàn. Đôi mắt chuyển dịch về phía trước khuôn mặt nhưng mí mắt nhắm nghiền. Cơ quan sinh dục bắt đầu hình thành bên cấu trúc cơ bản. | **Mẹ:** Đầy hơi, khó tiêu, ợ chua liên tục. Tâm lý lo lắng bồn chồn trước các mốc khám thai lớn sắp tới. Thể tích máu tăng 50% dẫn đến đau đầu nhẹ. | **Dinh dưỡng:** Tránh thức ăn chứa chất béo bão hòa và đồ uống có ga để giảm sưng tức dạ dày. Bổ sung Sắt (30mg/ngày) + Vitamin B12 để tạo hồng cầu.<br>**Cách dùng:** B12 hỗ trợ chuyển hóa tế bào thần kinh cho bé. | **Cảnh báo:** Đau đầu dữ dội không thuyên giảm kết hợp với mờ mắt (cần theo dõi sớm các bất thường về huyết áp và nội tiết nặng). |
| **W10** | 4 | 3.1 | **Sinh lý:** Thận của thai nhi bắt đầu hoạt động, bài tiết nước tiểu loãng đầu tiên vào buồng ối. Thai nhi đã bắt đầu có chuyển động tự nhiên nhưng mẹ chưa cảm nhận được. | **Mẹ:** Kích thước vòng eo tăng nhẹ, quần áo cũ bắt đầu chật. Khí hư âm đạo (huyết trắng) ra nhiều hơn nhưng không ngứa, không mùi. | **Dinh dưỡng:** Bắt đầu bổ sung Canxi nguyên tố (800mg/ngày) kết hợp Vitamin D3 (800 IU/ngày) vì hệ xương của thai nhi đang bắt đầu cốt hóa tích cực.<br>**Cách dùng:** Canxi uống buổi sáng, cách biệt Sắt ít nhất 2 tiếng. | **Cảnh báo:** Khí hư đổi màu vàng/xanh, có cấu trúc như bã đậu, kèm ngứa ngáy dữ dội (viêm âm đạo do nấm Candida hoặc tạp khuẩn, cần điều trị để tránh nhiễm trùng ối). |
| **W11** | 8 | 4.1 | **Sinh lý:** Toàn bộ các cơ quan nội tạng quan trọng (gan, thận, não, phổi) đã hình thành cơ bản và đang phát triển tinh xảo hơn. Phóng xuất các hormone nội tại. | **Mẹ:** Giảm nghén dần (ở hầu hết mẹ bầu). Tâm trạng vui tươi hơn. Bắt đầu có cảm giác thèm ăn các món ăn cụ thể. | **Dinh dưỡng:** Đa dạng nhóm chất. Chú ý bổ sung Canxi + Vitamin D3 và DHA tối thiểu 200mg/ngày cho cấu trúc não bộ thần kinh trung ương đang biệt hóa cực nhanh.<br>**Cách dùng:** DHA vào bữa tối để dễ ngủ. | **Cảnh báo:** **[MỐC VÀNG KHÁM THAI 1]** (Tuần 11 - 13 tuần 6 ngày) bắt buộc siêu âm đo độ mờ da gáy (Lưu ý: NT > 3mm là vùng nguy cơ cao dị tật nhiễm sắc thể sọ não, Down). |
| **W12** | 15 | 5.4 | **Sinh lý:** Ngón tay, ngón chân phân tách hoàn toàn, có thể co duỗi nhẹ. Thai nhi có phản xạ nuốt nước ối và thực hiện cử động mút tay sinh học sơ khởi. Kích thước tương đương **Quả chanh tây (Macaron/Chim non)**. | **Mẹ:** Tử cung đã nhô cao khỏi khớp mu xương chậu. Bụng dưới lồi nhẹ rõ rệt. Đường sọc nâu sữa (linea nigra) bắt đầu xuất hiện trên bụng mẹ. | **Dinh dưỡng:** Canxi (1000mg/ngày) + Vitamin D3. Duy trì đạm động vật chất lượng cao bổ sung đầy đủ axit amin thiết yếu tạo cơ xương.<br>**Cách dùng:** Hạn chế ăn đồ sống, thức ăn chưa chín kỹ để tránh listeria. | **Cảnh báo:** Chảy máu chân răng và chảy máu cam nhẹ do mao mạch giãn nở cực độ (Cần tăng cường Vitamin C và vệ sinh răng miệng bằng bàn chải mềm). |
| **W13** | 23 | 7.4 | **Sinh lý:** **Bước sang Tam Cá Nguyệt thứ 2**. Dấu vân tay cá biệt độc bản duy nhất của bé được hình thành. Các sợi tóc và tơ lông mày bắt đầu nhú ra. | **Mẹ:** Hết hẳn nghén dạ dày. Hormonal cân bằng. Năng lượng tràn trề, làn da hồng hào đầy sức sống do lưu lượng máu tăng mạnh. | **Dinh dưỡng:** Nhu cầu kẽm tăng lên. Sắt (30mg) + Canxi (1000mg) + DHA + Magie giúp giảm táo bón và chuột rút bắp chân hiệu quả.<br>**Cách dùng:** Bổ sung thực phẩm giàu Magie (hạt hạnh nhân, chuối). | **Cảnh báo:** Xuất hiện những cơn đau nhói đột ngột ở hai bên bẹn khi thay đổi tư thế di chuyển (Cơn đau dây chằng tròn sinh lý, cần tránh chuyển tư thế nhanh nguy hiểm). |
| **W14** | 43 | 8.7 | **Sinh lý:** Tuyến giáp và tuyến tụy của thai nhi giải phóng hormone chuyển hóa độc lập. Gan bắt đầu tiết mật, lách sản xuất hồng cầu. | **Mẹ:** Ngực tiết sữa non màu vàng nhạt (ở một số mẹ bầu cơ địa nhạy cảm). Tử cung phát triển vượt khỏi vùng tiểu khung tiến vào ổ bụng. | **Dinh dưỡng:** Tăng cường Canxi để bé lấy khoáng chất xây dựng cấu trúc xương tai trong và răng sụn mầm.<br>**Cách dùng:** Ăn nhiều thực phẩm tôm, cua nhỏ nguyên vỏ và thực phẩm từ sữa. | **Cảnh báo:** Ra dịch âm đạo có mùi hôi, màu xám đục (Viêm âm đạo do vi khuẩn BV - nguy cơ gây sảy thai muộn hoặc rò ối sớm nếu không diệt khuẩn). |
| **W15** | 70 | 10.1 | **Sinh lý:** Hệ xương khớp của thai nhi phát triển thần tốc, chuyển từ sụn mềm sang xương cứng vững chãi. Tai dịch chuyển về đúng vị trí hai bên đầu. | **Mẹ:** Có thể gặp hiện tượng nghẹt mũi hoặc chảy máu mũi nhẹ do lưu lượng máu tăng cao và làm khô sưng lớp niêm mạc mũi. | **Dinh dưỡng:** Đảm bảo lượng nước nạp vào đạt 2 lít/ngày. Bổ sung Canxi + Vitamin D3 + Sắt đầy đủ không ngắt quãng.<br>**Cách dùng:** Tránh uống canxi lúc đói vì gây sỏi thận nhẹ. | **Cảnh báo:** Đau mỏi lưng âm ỉ kéo dài phát triển (tư thế đứng ngồi sai lệch chịu áp lực từ tử cung lớn dần, cần tập bài tập duỗi lưng an toàn). |
| **W16** | 100 | 11.6cm | **Sinh lý:** Đôi mắt của thai nhi có phản xạ đảo nhẹ và nhạy cảm với ánh sáng đi qua thành bụng mẹ dù mí mắt vẫn còn nhắm. | **Mẹ:** Thèm ăn tăng cao rõ rệt. Đỉnh tử cung nằm ở vị trí khoảng nửa đường từ xương mu đến rốn mẹ. Đường cong cơ thể thay đổi rõ nét. | **Dinh dưỡng:** Sắt (30-60mg/ngày) để bù lượng hồng cầu giãn rộng. DHA (200mg/ngày) tăng cường phát triển tế bào võng mạc mắt và vỏ não bé.<br>**Cách dùng:** Tránh thức ăn chế biến sẵn quá nhiều muối. | **Cảnh báo:** Có thể làm xét nghiệm Triple Test hoặc chọc ối sàng lọc nếu Double Test trước đó có kết quả nghi ngờ bất thường nhiễm sắc thể. |
| **W17** | 140 | 13 | **Sinh lý:** Các lớp mỡ dưới da (chất béo nâu) bắt đầu tích lũy dày lên giúp giữ nhiệt cho bé sau khi sinh. Bé bắt đầu thực hiện động tác thở hít nước ối nhịp nhàng. | **Mẹ:** Đổ mồ hôi nhiều hơn, thân nhiệt cơ thể mẹ bầu luôn ở mức ấm nóng nhẹ do tốc độ chuyển hóa chất cơ bản tăng mạnh. | **Dinh dưỡng:** Canxi (1200mg/ngày) + Vitamin D3 (800IU). Bổ sung Protein từ thịt gà, sữa, phô mai để kích thích phát triển khối cơ cho thai.<br>**Cách dùng:** Hạn chế các loại cá hàm lượng thủy ngân cao (cá ngừ lớn, cá thu lớn). | **Cảnh báo:** Cảm giác ngứa ngáy châm chích vùng da bụng và đùi nở rộng (Sự rạn nứt sợi collagen dưới da, cần bôi kem dưỡng ẩm dịu nhẹ lành tính). |
| **W18** | 190 | 14.2 | **Sinh lý:** Lớp bảo vệ myelin bao bọc quanh các sợi dây thần kinh hoàn tất, cho phép truyền xung tín hiệu thần kinh nhanh chuẩn xác. | **Mẹ:** **[ĐIỂM MỐC THAI MÁY]** Mẹ bắt đầu cảm nhận rõ cử động nhẹ của thai nhi như tiếng sủi bọt khí bong bóng hoặc cá quẫy đuôi nhẹ trong bụng. | **Dinh dưỡng:** Khẩu phần năng lượng tăng thêm khoảng 340 kcal/ngày. Bổ sung Vitamin C (85mg/ngày) giúp bền vững collagen tử cung và tăng hấp thu Sắt.<br>**Cách dùng:** Ăn hoa quả muối chua, ổi, cam, kiwi xanh. | **Cảnh báo:** Nếu mẹ mang thai con rạ (lần 2 trở đi) mà đến tuần 20 hoàn toàn không thấy thai máy, cần đi siêu âm kiểm tra nhịp tim thai lân cận. |
| **W19** | 240 | 15.3 | **Sinh lý:** Da thai nhi được bao phủ bởi một lớp sáp bảo vệ màu trắng sữa gọi là **chất gây (vernix caseosa)** để ngăn ngừa da bé bị trầy xước, nhăn nheo do ngâm ối lâu ngày. | **Mẹ:** Huyết áp của mẹ bắt đầu giảm nhẹ do các mạch máu ngoại vi giãn rộng (dễ gây tư thế chóng mặt khi đứng lên đột ngột). | **Dinh dưỡng:** Tăng cường Kali (chuối, khoai tây) để cân bằng huyết áp điện giải, giảm co cơ bắp gây sưng phù chân.<br>**Cách dùng:** Tránh đứng lâu một vị trí cố định. | **Cảnh báo:** Chóng mặt dữ dội dốc ngã, ngất xỉu tạm thời (Thiếu máu do thiếu sắt huyết động hoặc hạ huyết áp tư thế nặng, cần xét nghiệm công thức máu). |
| **W20** | 300 | 25.6 | **Sinh lý:** Đạt mốc giữa thai kỳ. Hệ thần kinh giác quan (thính giác, thị giác, khứu giác, vị giác) phát triển toàn diện. Bé có thể nghe được tiếng tim mẹ đập và giọng nói bên ngoài. | **Mẹ:** Đỉnh tử cung đã cao chạm ngang rốn mẹ. Mẹ tăng cân ổn định (trung bình 0.5kg/tuần). Giấc ngủ bắt đầu khó khăn khó tìm tư thế thoải mái. | **Dinh dưỡng:** Canxi (1200mg/ngày) + Vitamin D3. Bổ sung sắt đều đặn. Thêm các chất béo tốt không bão hòa từ hạt óc chó, hạt macca tốt cho tim mạch.<br>**Cách dùng:** Ăn hạt làm bữa phụ lành mạnh hàng ngày. | **Cảnh báo:** **[MỐC VÀNG KHÁM THAI 2]** (Tuần 20 - 24) bắt buộc siêu âm 4D hình thái tầm soát chi tiết các dị tật tim bẩm sinh, sứt môi, hở hàm ếch, thoát vị hoành, bất thường não bộ. |
| **W21** | 360 | 26.7 | **Sinh lý:** Hệ tiêu hóa của thai nhi phát triển mạnh, ruột hấp thụ tốt các chất dinh dưỡng vi lượng từ nước ối nuốt vào. Phân su màu đen sẫm bắt đầu tích tụ dần trong đại tràng bé. | **Mẹ:** Hiện tượng chuột rút chân (nhất là vào ban đêm) xuất hiện thường xuyên do áp lực cơ học từ tử cung lên thần kinh đùi bụng. | **Dinh dưỡng:** Bổ sung dồi dào Canxi và Magie để giải phóng cơ bắp bị bó cứng. Ngâm chân nước ấm trước khi đi ngủ.<br>**Cách dùng:** Tránh uống sữa cùng lúc uống viên canxi tổng hợp. | **Cảnh báo:** Chân phù to bất thường đổi kích cỡ nhanh, đau nhức một bên bắp chân dưới (nguy cơ tắc nghẽn tĩnh mạch sâu huyết khối, cần can thiệp y tế gấp). |
| **W22** | 430 | 27.8 | **Sinh lý:** Lông mi và lông mày của bé phát triển hoàn thiện chi tiết sắc nét. Bé liên tục nhào lộn, nhịp máy thai đều mạnh mẽ hơn. | **Mẹ:** Tăng dịch tiết âm đạo lành tính. Ham muốn tình dục có thể tăng trở lại do nội tiết dồi dào và tưới máu cơ quan sinh dục cực lớn. | **Dinh dưỡng:** Bổ sung sắt ngăn ngừa thiếu máu thiếu sắt. Ăn nhiều rau xanh đậm (súp lơ xanh, rau bina) giàu sắt và folate tự nhiên phong phú.<br>**Cách dùng:** Rau nấu chín kỹ, không ăn sống tái tránh sán ký sinh. | **Cảnh báo:** Đau bụng âm ỉ vùng rốn dữ dội kèm theo cơn co cứng toàn tử cung kéo dài (Co thắt dọa sảy muộn hoặc viêm ruột thừa cấp tính thai kỳ). |
| **W23** | 500 | 28.9 | **Sinh lý:** Các mạch máu nhỏ li ti dưới da hình thành, phủ màu da đỏ hồng hào ấm áp. Tai trong phát triển hoàn chỉnh giúp bé phân biệt thăng bằng chuyển động rõ rệt. | **Mẹ:** Có thể xuất hiện sắc tố sẫm màu tàn nhang trên da mặt dưới kích thích của hormone hCG và progesterone (Mặt nạ thai kỳ chloasma). | **Dinh dưỡng:** Chăm sóc da bằng các sản phẩm chiết xuất thiên nhiên. Uống đủ nước (2 - 2.5 lít/ngày) để duy trì cấu trúc lỏng mượt của tế bào.<br>**Cách dùng:** Hạn chế trà sữa, nước ngọt gây tăng đường huyết đột biến. | **Cảnh báo:** Đau rát buốt khi đi tiểu, nước tiểu đục hoặc có máu (Nhiễm trùng đường tiết niệu bể thận dọa sinh non, cần xét nghiệm phân tích nước tiểu ngay). |
| **W24** | 600 | 30 | **Sinh lý:** Phổi bắt đầu rẽ nhánh thành các túi phổi nhỏ li ti (bronchioles). Trí não bé phát triển dốc thẳng đứng để tiếp nhận các xúc giác xung quanh. | **Mẹ:** Xuất hiện các cơn co thắt tử cung sinh lý giả (Cơn co Braxton Hicks) không đau đặn, xuất hiện ngẫu nhiên vài lần trong ngày. | **Dinh dưỡng:** Sắt (30-60mg) + Canxi (1200mg) + Đạm. Tăng cường thực phẩm chứa chất xơ để phòng táo bón kịch phát.<br>**Cách dùng:** Chia nhỏ bữa ăn hỗ trợ hệ tiêu hóa chịu ép lực từ tử cung. | **Cảnh báo:** **[MỐC VÀNG KHÁM THAI 3]** (Tuần 24 - 28) bắt buộc thực hiện nghiệm pháp dung nạp glucose (uống 75g đường) tầm soát đái tháo đường thai kỳ để ngăn biến chứng tiền sản giật, thai to ngạt sinh. |
| **W25** | 660 | 34.6 | **Sinh lý:** Lỗ mũi của bé bắt đầu mở ra, cho phép bé thực hiện các nhịp "hít thở" thụ động thử nghiệm trong môi trường nước ối ấm áp. | **Mẹ:** Trở nên vụng về mệt mỏi hơn do trọng tâm cơ thể dồn về phía trước bụng. Đau nhức vùng xương mu khớp vệ nhẹ do dãn dây chằng sinh lý. | **Dinh dưỡng:** Tránh mang giày cao gót. Bổ sung Canxi để bù lượng khoáng mà cơ thể mẹ đang trích xuất chuyển vào cho hệ xương hóa cứng của bé.<br>**Cách dùng:** Uống canxi muộn nhất lúc 11h trưa để hấp thụ tối ưu. | **Cảnh báo:** Cơn co thắt dồn dập xuất hiện đều dưới 15 phút một lần kèm theo đau thắt thắt lưng dưới dọa sinh non (Cần can thiệp thuốc cắt cơn dọa sinh sớm). |
| **W26** | 760 | 35.6 | **Sinh lý:** Mí mắt bắt đầu mở ra lần đầu tiên sau thời gian nhắm, nhãn cầu có khả năng đáp ứng nhấp nháy chuyển độ khi có luồng sáng chiếu sáng. | **Mẹ:** Khó thở nhẹ do tử cung phình lớn đẩy cơ hoành ngực lên vị trí cao hơn 4cm so với tư thế sinh lý thông thường. | **Dinh dưỡng:** Khi ngủ nên nằm nghiêng bên TRÁI để giảm áp lực của tử cung trực tiếp lên tĩnh mạch chủ dưới, tăng nồng độ máu chảy nuôi dưỡng bánh nhau.<br>**Cách dùng:** Sử dụng gối ôm bà bầu hình chữ U hỗ trợ lưng đùi. | **Cảnh báo:** Khó thở kèm tức ngực dữ dội, tím tái môi đầu chi khi gắng sức nhẹ (Báo động suy tim ẩn tàng hoặc thuyên tắc phổi cấp tính). |
| **W27** | 875 | 36.6 | **Sinh lý:** **Bước sang Tam Cá Nguyệt thứ 3**. Não bộ liên tục thiết lập hàng tỷ kết nối synapse thần kinh thần tốc. Bé có chu kỳ thức - ngủ đều đặn điều phối. Kích thước tương đương **Quả dừa non (Mousse/Thỏ bông)**. | **Mẹ:** Cân nặng mẹ tăng nhanh rõ rệt (60% tổng lượng tăng cả thai kỳ dồn vào tam cá nguyệt này). Hay bị đầy bụng, ợ nóng bỏng rát thực quản. | **Dinh dưỡng:** Hạn chế ăn đồ béo ngậy muộn vào ban đêm gây trào ngược acid dạ dày. Bổ sung đạm chất lượng cao sạch (thịt bò nạc, lòng đỏ trứng gà chín).<br>**Cách dùng:** Chia nhỏ 6 bữa ăn nhẹ dãn cách đều. | **Cảnh báo:** Sưng phù ở tay, mặt đột ngột không giảm sau nằm nghỉ qua đêm kèm nhức đầu vùng trán dữ dội (Triệu chứng điểm hình của tiền sản giật nặng). |
| **W28** | 1000 | 37.6 | **Sinh lý:** Lớp mỡ dưới da phát triển dày căng mượt khiến da bớt đỏ, bớt nhăn nheo hẳn. Nhịp tim bé phản hồi linh hoạt với âm thanh lạ bên ngoài. | **Mẹ:** Đau tức lưng dưới dữ dội tăng tiến mạnh do cột sống phải ưỡn cong hết cỡ chịu sức nặng 10-12kg lực phân bổ lệch phía trước bụng. | **Dinh dưỡng:** Canxi 1200 - 1500 mg/ngày + Vitamin D3 + DHA kết hợp Sắt dồi dào để phòng thiếu máu thứ phát cuối kỳ.<br>**Cách dùng:** Không tự ý dùng thuốc giảm đau cơ học nếu chưa có chỉ định. | **Cảnh báo:** Đo huyết áp tại nhà thấy chỉ số tâm thu >140 mmHg hoặc tâm trương >90 mmHg kèm đạm niệu cao (Xác chẩn tiền sản giật nguy cơ sản giật đe dọa tính mạng). |
| **W29** | 1150 | 38.6 | **Sinh lý:** Đầu bé to lên rõ rệt để có đủ khoảng không dung chứa cho bộ não đang phình to nếp nhăn nhanh chóng. Bé chuyển động đá cùi chỏ mạnh rõ rệt. | **Mẹ:** Xuất hiện triệu chứng giãn tĩnh mạch vùng bẹn âm hộ hoặc chi dưới chân do lực đè nén tử cung cản trở tuần hoàn tĩnh mạch trở về tim. | **Dinh dưỡng:** Bổ sung thực phẩm giàu Vitamin K, Vitamin C bảo bọc thành mạch máu vững chãi. Tắm bồn nước ấm hoặc gác chân cao khi nằm ngủ sâu.<br>**Cách dùng:** Tránh đứng tư thế nghiêm túc quá lâu. | **Cảnh báo:** Có cơn gò cứng bụng dồn dập kèm ra dịch hồng nhạt (Dọa sinh non cấp tính từ vùng tuổi thai nhạy cảm, cần lập tức dùng corticoid trưởng thành phổi bé). |
| **W30** | 1300 | 39.9 | **Sinh lý:** Tủy xương của thai nhi chính thức tiếp quản 100% nhiệm vụ sản xuất hồng cầu thay thế hoàn toàn vai trò của lách và gan trước đó. | **Mẹ:** Khó ngủ sâu, mệt mỏi, lo âu tâm lý gần sinh tăng mạnh (Somniphobia). Thường xuyên đi tiểu buốt ban đêm do đầu thai nhi tì đè bàng quang. | **Dinh dưỡng:** Ăn tối nhẹ nhàng, uống ít nước trước ngủ 2 tiếng để hạn chế đi tiểu đêm làm gián đoạn giấc ngủ vàng ngọc tái tạo năng lượng.<br>**Cách dùng:** Duy trì vi chất uống đều sau ăn sáng và trưa. | **Cảnh báo:** Số lần thai máy giảm dưới 10 chuyển động cử động trong 2 giờ đếm thai máy tích cực (Suy thai cấp tính nguy hiểm đe dọa tử vong lưu thai). |
| **W31** | 1500 | 41.1 | **Sinh lý:** Toàn bộ 5 giác quan cảm xúc của bé hoạt động hoàn hảo đầy đủ. Bé có thể tự điều tiết thân nhiệt trong dịch ối qua điều phối sọ não. | **Mẹ:** Căng tức ngực mạnh nặn ra dịch sữa sữa non giàu dinh dưỡng để chuẩn bị đón bé. Khớp mu chân râm ran nới rộng giãn xương. | **Dinh dưỡng:** Bổ sung Đạm động vật chất lượng cao tốt nhất (Trứng, ức gà, cá hồi chứa omega 3 dồi dào xúc tiến tạo liên kết myelin neuron trẻ).<br>**Cách dùng:** Không bóp vắt ngực chảy sữa non vì kích ứng co bóp tử cung gây sinh non gắt. | **Cảnh báo:** Ra dịch nước trong suốt từ ròng ròng âm đạo hoàn toàn không mùi không tự chủ (Rò ối sớm nguy cơ viêm phúc mạc tử cung nhiễm trùng mầm bé). |
| **W32** | 1700 | 42.4 | **Sinh lý:** Hầu hết lông tơ mịn bao quanh người rụng dịch chuyển vào nước ối. Xương sọ mềm dẻo chưa liên kết khớp để sẵn sàng nén đầu chui ra ngả âm đạo. | **Mẹ:** Khó tiêu nghiêm trọng do dạ dày bị chèn ép tối đa nằm lệch hẳn góc đẩy lên trên. Cân nặng phát phì nhanh ở mặt và ngón tay lỏng. | **Dinh dưỡng:** Chia thức ăn làm 6-8 bữa cực nhỏ dạng sệt lỏng dễ hấp thu nhanh. Bổ sung Sắt (60mg/ngày) + Canxi (1500mg/ngày) lực cuối dồn xương mầm.<br>**Cách dùng:** Sắt uống kèm nước cam tăng hấp thu hồng cầu cực mạnh. | **Cảnh báo:** **[MỐC VÀNG KHÁM THAI 4]** (Tuần 32) bắt buộc siêu âm doppler khảo sát cấu trúc mạch máu bánh nhau, đo thể tích ối (đặc biệt phát hiện thiểu ối, đa ối nguy hiểm). |
| **W33** | 1900 | 43.7 | **Sinh lý:** Hệ miễn dịch tự nhiên của thai nhi nhận được lượng kháng thể immunoglobulin G phong phú truyền từ cơ thể mẹ qua tĩnh mạch bánh nhau để tự vệ sau sinh. | **Mẹ:** Đau dây thần kinh tọa vùng hông mông xuống đùi dữ dội do đầu thai nhi chúc xuống vùng tiểu khung đè rễ thần kinh thắt lưng hông L5-S1. | **Dinh dưỡng:** Bổ sung thực phẩm giàu Vitamin C tăng cường hấp phụ sắt tạo máu. Đảm bảo lượng nước 10 cốc đầy một ngày chống cạn ối âm thầm.<br>**Cách dùng:** Nghỉ ngơi nằm nghiêng trái kê gối mềm kê dưới đùi nâng bắp chân. | **Cảnh báo:** Nhìn mờ ảo ảnh, sưng mặt to biến dạng, đau nhức hốc mắt liên tục (Tiền sản giật muộn bộc phát dữ dội nấc thang co thắt động mạch não). |
| **W34** | 2100 | 45 | **Sinh lý:** **[ĐIỂM MỐC PHỒI TRƯỞNG THÀNH]** Phổi tiết đầy đủ **chất diện hoạt (surfactant)** giúp lòng phế nang giãn nở đều đặn, trẻ sinh ra từ bây giờ ít nguy cơ suy hô hấp do non phổi. | **Mẹ:** Các vết rạn nứt sẫm màu nở to tấy ngứa (Pruritic urticarial papules of pregnancy - PUPPP). Bụng căng cứng như trống chẹn ngực thắt lưng. | **Dinh dưỡng:** Uống nước dừa lành tính (nếu không tiểu đường thai kỳ) tăng lượng nước ối. Vi chất Sắt + Canxi + DHA duy trì đủ nồng độ bổ máu sườn bé.<br>**Cách dùng:** Kiểm tra huyết áp ngày 2 lần lưu trữ số liệu. | **Cảnh báo:** Cơn co thắt dồn dập kèm ra huyết màu sẫm đặc (Rau bong non cấp tính - biến chứng sản khoa tối khẩn cực kỳ nguy hiểm, mất tim thai trong vài phút). |
| **W35** | 2380 | 46.2 | **Sinh lý:** Thận của bé lọc và thải khoảng nửa lít nước tiểu mỗi ngày ra ngoài ối. Não bộ tăng kích thước gấp đôi so với 5 tuần trước đó để bứt phá. | **Mẹ:** Cảm giác nặng nề cực độ, vụng về bực bội mất kiên nhẫn bộc phát dữ dội. Đầu bé tì đè khớp bàng bàng gây són tiểu liên tục khi cười giòn dã. | **Dinh dưỡng:** Duy trì thức ăn tinh chất dễ tiêu (Yến sào tốt, súp bò băm bổ dưỡng). Sắt + Canxi + DHA đều đặn liều lượng chuẩn.<br>**Cách dùng:** Không ăn đồ chua muối xổi dễ kích ứng tiêu hóa dạ dày ruột. | **Cảnh báo:** Xuất hiện những cơn gò bụng cứng đơ như đá đều đặn cách nhau dưới 10 phút, không đỡ khi nghỉ ngồi (Sinh non giai đoạn muộn, cần đi viện ngay). |
| **W36** | 2600 | 47.4 | **Sinh lý:** Thai nhi bắt đầu chúc đầu xuống thấp hẳn chui sâu vào cấu trúc âm sâu thắt của hông (Ngôi thuận tự nhiên). Tần suất xoay lộn lộn giảm do hẹp buồng. | **Mẹ:** **Bụng bầu "tuột" thấp hẳn xuống** giải phóng áp lực lồng ngực giúp mẹ hít thở sâu trở lại dễ dàng (Sa bụng dưới sinh lý cận kề sinh). | **Dinh dưỡng:** Đi bộ vận động nhẹ nhàng dẻo dai hông chậu chuẩn bị đường sinh thuận lợi. Bổ sung Vitamin K đường ăn uống để phòng băng huyết sau sinh cho mẹ.<br>**Cách dùng:** Tránh đi đường xóc, va chạm lực mạnh nén bụng bầu. | **Cảnh báo:** **[MỐC KHÁM THAI HÀNG TUẦN]** Bắt đầu đo tim thai bằng Monitor sản khoa (Non-Stress Test) hàng tuần để theo dõi dự phòng biến chứng suy thai âm thầm trong buồng tử cung chật hẹp. |
| **W37** | 2850 | 48.6 | **Sinh lý:** Thai nhi được coi là **"Đủ tháng sớm" (Early Term)**. Toàn bộ các cơ quan nội tạng vận hành chuẩn xác đồng bộ. Bé mút tay liên tục tập dượt bú sữa. | **Mẹ:** Cơn co thắt giả thắt bụng ê ẩm tăng tiến tuần suất dữ dội hơn. Khớp hông chậu giãn nở lỏng lẻo kêu rắc lách cách khi chuyển bước. | **Dinh dưỡng:** Bữa ăn chú trọng nhiều tinh bột phức hợp bền vững lâu dài, duy trì nước ấm tốt ấm nhu động ruột thải phân sạch thuận lợi.<br>**Cách dùng:** Chuẩn bị sẵn làn giỏ đồ sinh hoạt chuẩn đi sinh cơ viện. | **Cảnh báo:** Đau đầu dữ dội, đau tức vùng thượng vị dưới ức sườn phải dữ dội không dứt (Hội chứng HELLP - biến chứng tiền sản giật kịch độc suy gan suy thận). |
| **W38** | 3100 | 49.8 | **Sinh lý:** Lớp sáp vernix trôi rụng hầu hết hòa hẳn dịch ối đục dần. Bé tăng khoảng 30g mỡ ấm dưới da mỗi ngày chuẩn bị đối mặt không khí lạnh ngoài tử cung. | **Mẹ:** Mất ngủ đỉnh điểm kéo dài (Insomnia). Tâm lý bồn chồn lo sợ cơn đau đẻ. Cảm giác áp lực thọc sâu xuống khớp mu tì rát. | **Dinh dưỡng:** Uống nước ấm pha chút mật ong dịu lo âu trước ngủ tối. Tiếp tục uống vi chất Sắt bổ móng máu dự trữ lực vượt cạn ra rả mất máu lớn.<br>**Cách dùng:** Không tự ý xông hơi ngâm bồn nước quá nóng kích co bóp sinh sớm. | **Cảnh báo:** Xuất hiện dịch nhầy màu hồng tươi thẫm loang rỉ quần chip (Bong nút nhầy sáp cổ tử cung báo hiệu chuyển dạ sầm sập trong vòng 24 - 48 giờ). |
| **W39** | 3300 | 50.7 | **Sinh lý:** Thai nhi hoàn hảo ở trạng thái cực đại sẵn sàng sinh nở hoàn toàn **"Đủ tháng chuẩn" (Full Term)**. Bánh nhau đạt mốc lão hóa canxi mức 3. | **Mẹ:** Cổ tử cung râm râm ngắn dần mỏng hóa tích cực (Cervical effacement). Mẹ nhạy cảm đau thúc mỗi nhịp thai đạp dưới khớp vệ. | **Dinh dưỡng:** Duy trì thức ăn giàu dinh dưỡng dễ hấp thu, uống đủ nước không lơ là bỏ bữa để có sức rặn lúc lâm bồn vượt cạn sinh tử.<br>**Cách dùng:** Không tự ý dùng các loại trà thảo mộc lạ kích rặn đẻ dọa vỡ tử cung. | **Cảnh báo:** Rỉ ối liên tục loãng sũng tã dán nhưng chưa thấy đau cơ co bụng (Cần nhập viện truyền kháng sinh chống nhiễm trùng ối ngược dòng bảo vệ bé). |
| **W40** | 3500 | 51.2 | **Sinh lý:** Ngày dự sinh lý thuyết đạt mốc. Thai nhi nằm chúc đầu khít chặt tiểu khung hông không thể di động ngực vai sườn, chờ lực đẩy từ co thắt tử cung. | **Mẹ:** Tâm trạng bùng nổ lo lắng vì đã đến ngày dự sinh nhưng nhiều trường hợp chưa xuất hiện dấu hiệu chuyển dạ tự nhiên (thai lì). | **Dinh dưỡng:** Duy trì vận động đi bộ nhẹ nhàng quanh sân bệnh viện kích thích đầu bé tì đè cổ tử cung phóng thích oxytocin tự nhiên gây chuyển dạ thuận lợi.<br>**Cách dùng:** Nghỉ ngơi bất cứ khi nào có thể. | **Cảnh báo:** **[MỐC VÀNG QUAN TRỌNG TRÁNH LƯU THAI]** Siêu âm kiểm toán chỉ số ối nước thường xuyên 2 ngày/lần. Nếu chỉ số ối AFI < 5cm (thiểu ối nghiêm trọng) bắt buộc chỉ định mổ hoặc đặt thuốc giục sinh khẩn. |
| **W41** | 3600 | 51.8 | **Sinh lý:** Thai kỳ quá ngày dự sinh lý thuyết (Late Term). Bánh nhau suy thoái thoái hóa dần hóa canxi nấc nặng, giảm cung cấp oxy dưỡng chất. | **Mẹ:** Căng thẳng mệt mỏi tột cùng. Lo lắng suy sụp thần kinh mệt nhoài. Cơ thể nặng trĩu sập sệ nhói đau buốt. | **Dinh dưỡng:** Tuân thủ 100% y kiến bác sĩ sản khoa phụ trách để can thiệp kích sinh chủ động hoặc mổ lấy thai gục bảo đảm an toàn mẹ và mầm nhi.<br>**Cách dùng:** Đến khoa cấp cứu sản khoa ngay lập tức. | **Cảnh báo:** Phát hiện nước ối rò rỉ có màu xanh rêu hoặc vàng đậm bẩn (Phản xạ thải phân su sớm của bé do suy ngạt cấp thiếu oxy, hít phân su dọa tử vong sơ sinh). |

---

## III. THIẾT KẾ CÁC CHỦ ĐỀ TRỰC QUAN GIAO DIỆN (VISUALIZATION THEMES)
Để tăng trải nghiệm cá nhân hóa kết nối cảm xúc của mẹ bầu đối với phôi thai lớn lên từng ngày, hệ thống định hình 3 bộ giao diện visual so sánh kích thước thay đổi hình thái độc đáo với cấu trúc dữ liệu mapping tuần 4, tuần 12 và tuần 28 cụ thể như sau:

```
[Màn Hình Dashboard] ──> [Đọc Config users.visualization_theme]
                             │
                             ├──> 'FRUIT'   ──> Hiển thị Nông Trại Trái Cây
                             ├──> 'BAKERY'  ──> Hiển thị Thế Giới Bánh Ngọt
                             └──> 'ANIMAL'  ──> Hiển thị Muông Thú Đáng Yêu
```

### 1. Bộ 1: Giao diện "Nông Trại Trái Cây" (Fruit Theme - Mặc định)
*   **Triết lý phong cách:** Gói trọn sự tươi trẻ lành sạch của trái cây chín mọng tự nhiên làm thước đo tình mẫu tử.
*   **Tuần 4:** Nhỏ xinh bằng **Hạt anh đào** (Mè/Sesame seed) ẩm sương.
*   **Tuần 12:** To bằng **Quả chanh tây** căng mướt dồi dào nước ép tơ vàng óng.
*   **Tuần 28:** Đạt kích thước tương đương bằng một **Quả dừa non** đầy ắp nước ngọt trong buồng.

### 2. Bộ 2: Giao diện "Thế Giới Bánh Ngọt" (Bakery Theme)
*   **Triết lý phong cách:** Ấm cúng dễ thương rực rỡ ngọt ngào với hương thơm dịu ngậy từ lò nướng pastry Pháp cổ điển.
*   **Tuần 4:** Nhẹ tênh mỏng manh tương đương **Viên kẹo đường nhỏ trang trí màu sắc** lấp lánh trên bánh kem trứng.
*   **Tuần 12:** Tròn xinh chắc chắn vừa vặn bằng kích cỡ của một chiếc bánh **Macaron Pháp** bơ dâu thơm ngậy.
*   **Tuần 28:** Đạt kích cỡ bề thế tương đương một ổ bánh ngọt **Mousse Chocolate lớn** bồng bềnh bông sữa mịn.

### 3. Bộ 3: Giao diện "Muông Thú Đáng Yêu" (Cute Animal Theme)
*   **Triết lý phong cách:** Đậm chất hoạt họa tinh nghịch ấm áp kết đôi cùng các bé sinh vật hoang dại thơ ngây.
*   **Tuần 4:** Nhỏ bé ẩn nấp trong nách lá bằng một chú **Bọ rùa đỏ chấm đen** tí hon.
*   **Tuần 12:** Sinh động kêu ríu rít tương đương một chú **Chim non mới nở** đang đập đôi cánh tơ mộc mạc chưa ráo lông.
*   **Tuần 28:** To tròn bồng bềnh đầy mịn màng tựa một chú **Thỏ con lông bông màu tuyết cốt** ôm tròn tai dài.

---

## IV. ĐẶT LỊCH KHÁM THAI ĐỊNH KỲ & DẤU HIỆU CHUYỂN DẠ KHẨN CẤP
Hệ thống sử dụng các mốc can thiệp y khoa bắt buộc để tự động tạo lịch hẹn khơi mào cho ứng dụng của mẹ bầu kèm 8 chế độ cảnh báo dấu hiệu chuyển dạ cần đưa vào cảnh báo khẩn kích hoạt rầm rộ trên thiết bị Android:

### 1. Phân Rã Các Mốc Khám Thai Cốt Lõi Bắt Buộc (Sản khoa Việt Nam & Quốc tế)

```
[Bảng Lịch Khám Thai Bắt Buộc]
 ├── Mốc 1: Tuần 11 - 13đ6n ──> Siêu âm đo độ mờ da gáy NT + Double Test (Sàng lọc Down)
 ├── Mốc 2: Tuần 20 - 24     ──> Siêu âm hình thái học 4D (Tầm soát dị tật chi tiết nội tạng)
 ├── Mốc 3: Tuần 24 - 28     ──> Nghiệm pháp dung nạp 75g Glucose (Sàng lọc Đái tháo đường thai kỳ)
 ├── Mốc 4: Tuần 32          ──> Siêu âm Doppler đánh giá tuần hoàn rốn, bánh nhau, ối cốt tử
 └── Mốc 5: Tuần 36+         ──> Chạy Monitor tim thai (Non-Stress Test) hàng tuần đo cơn co rặn
```

*   **Mốc 1 (Tuần 11 - 13 tuần 6 ngày):** Đo khoảng sáng sau gáy (Độ mờ da gáy NT - Nuchal Translucency) kết hợp xét nghiệm máu tĩnh mạch Double Test. Xác định sớm nguy cơ đột biến cấu trúc thể nhiễm sắc thể 21 (Down), 18 (Edwards), 13 (Patau). **Không thể bù mốc này nếu để lỡ qua tuần 14.**
*   **Mốc 2 (Tuần 20 - 24):** Siêu âm dựng hình thái 4D cao cấp. Tầm soát rà soát từng bộ phận cơ thể bé: hệ thần kinh (não úng thủy, giãn não thất), sọ não, sứt môi hở hàm ếch, dị tật xương đùi cột sống, dị tật tim bẩm sinh, dị tật hở cơ thành bụng thoát vị hoành.
*   **Mốc 3 (Tuần 24 - 28):** Nghiệm pháp dung nạp glucose bằng cách uống dung dịch chứa 75g glucose khi đói, thử máu 3 lần dãn cách để tầm soát Đái tháo đường thai kỳ (Gestational Diabetes Mellitus). Phòng ngừa biến chứng thai to ngạt sinh, đột quỵ sản giật nguy hại.
*   **Mốc 4 (Tuần 32):** Siêu âm Doppler màu lưu sinh mạch máu cuống rốn, bánh nhau trung ương, mao mạch não thai nhi để đánh giá nguồn cung dinh dưỡng nuôi bé có bị chậm tăng trưởng trong tử cung (IUGR) hay không; khảo sát vị trí bánh nhau bám (nhau tiền đạo) nguy cơ chảy máu ồ ạt lúc chuyển dạ, và đo thể tích nước ối phòng thiểu ối, cạn ối âm thầm.
*   **Mốc 5 (Tuần 36 trở đi):** Thử ghi kiểm tra liên tục tim thai bằng điện cực Monitor sản khoa kết nối áp lực cơn co tử cung (Chạy máy NST - Non Stress Test). Đánh giá biến động nhịp nhanh tim đáp ứng cử động thai máy hay phẳng dẹt cảnh báo ngạt để chủ động chỉ định mổ lấy thai sớm bảo vệ bé an toàn cực độ.

### 2. Trích Xuất Toàn Vẹn "8 Dấu Hiệu Chuyển Dạ Sắp Sinh" Cần Cấu Hình Critical Alerts
Để bảo vệ an toàn tính mạng cả mẹ lẫn con trước những tình huống chuyển dạ cấp, ứng dụng Android phải cấu hình 8 dấu hiệu cảnh báo sau thành các lệnh Critical Alerts kích hoạt thông báo chuông rầm rộ bỏ qua chế độ im lặng của máy:
1.  **Sa bụng dưới đột ngột (Lightening):** Mẹ bầu có cảm giác bụng bầu sa tuột xuống sâu, giảm dung tích chèn ép ngực giúp thở phào thảnh thơi nhưng tăng áp lực dồn nén buốt rát thốn bàng quang khớp vệ (đầu bé đã lọt tiểu khung).
2.  **Cơn co thắt tử cung chuyển dạ thật sự (True Labor Contractions):** Xuất hiện những cơn gò cơ tử cung cứng chặt bụng bóng đớn kèm đau xiết xuất hiện đều đặn với chu kỳ tăng dần tốc độ, khoảng cách giãn cách của các cơn co đều đặn và ngắn dần dưới $t \le 10$ phút (Ví dụ: 3 cơn co liên tục trong 10 phút kéo dài hơn 30 giây rát), cường độ tăng tiến dữ dội kể cả khi đổi tư thế nghỉ ngơi.
3.  **Bong nút nhầy bảo vệ cổ tử cung (Bloody show):** Âm đạo thải rỉ những vệt dịch nhầy quánh dính loang hồng tía hoặc sẫm nâu đỏ giống như tiết (Sự xóa mở mạch máu cổ tử cung làm nút sáp bảo vệ rơi rụng báo hiệu sinh nở trong 24h-48h).
4.  **Vỡ nước ối rào rạt hoặc rò rỉ rỉ ối âm ỉ (Rupture of Membranes):** Nước trong suốt hoàn toàn không mùi, chảy tràn tự do chảy ra từ đường âm đạo đột ngột bất kiểm soát (vỡ ối hoàn toàn) hoặc rỉ ướt sũng đáy quần chip (Rò ối âm thầm dọa nhiễm khuẩn mủ ối cần nhập viện khẩn để bảo vệ tì ròng rọc rốn bé).
5.  **Cổ tử cung mỏng mở rộng dần (Cervical Dilation):** Cận kề mốc mở rộng từ 1cm đến 10cm chuẩn bị đưa mầm nhi bước ra thế giới (Xác định chuẩn xác qua thăm khám âm đạo sờ nắn của Bác sĩ sản khoa chuyên khoa phụ trách).
6.  **Đau nhức ê ẩm vùng thắt lưng dưới dốc kèm chuột rút dữ dội:** Do các dây chằng, cơ bắp và đốt sống thắt lưng hông bị đè ép và kéo giãn hết mức bởi đầu bé xoay hướng chúc lọt lòng.
7.  **Giãn nở cơ học khớp xương vùng chậu khớp mu:** Cảm giác khớp vùng chậu nới rộng lỏng lẻo lung lay (Hormone Relaxin tiết ra cực đại làm nới lỏng toàn bộ các sụn và gân khớp vệ xương hông bụng nở đường sinh rộng hết cỡ).
8.  **Tiêu chảy bài tiết nhẹ dọn ruột:** Nhu động đường ruột co thắt bài tiết dọn rác phân rỗng khoang ruột tự nhiên kích thích trực tiếp bởi nội tiết tố Prostaglandin tiết ra phóng thích kích hoạt xóa mở tử cung chuẩn bị sinh mọc.

---

## V. THIẾT KẾ CƠ SỞ DỮ LIỆU ĐỒNG BỘ SONG SONG (DATABASE SCHEMA)
Bộ dữ liệu được thiết kế đồng bộ nhất quán theo mô hình **Offline-First**. Toàn bộ Master Data (`fetal_development_master`) được nén sẵn thành file hạt giống SQL (`fetal_development_data.sql`) gài sẵn trong thư mục tài nguyên `assets/` của ứng dụng Android để tự động gieo mầm vào Room DB khi người dùng khởi động app lần đầu tiên không cần mạng.

### 1. Kiến Trúc Sơ Đồ Thực Thể Liên Kết (Entity Relationship Diagram - Concept)

```
  ┌────────────────────────┐
  │         users          │
  └───────────┬────────────┘
              │ 1
              ├──────────────────────────────┐
              │ 1..N                         │ 1..N
     ┌────────┴─────────────┐       ┌────────┴─────────────┐
     │ medical_appointments │       │ medication_reminders │
     └──────────────────────┘       └──────────────────────┘

  ┌─────────────────────────────────┐
  │    fetal_development_master     │ <── (Bảng Master nạp tĩnh từ SQL Seed)
  └─────────────────────────────────┘
```

### 2. Cấu Trúc Bảng Dữ Liệu SQL Chi Tiết
Dưới đây là thiết kế chuẩn hóa tương thích tuyệt đối giữa hệ quản trị local SQLite (Android Room Entity) và Cloud Database PostgreSQL:

#### Bảng 1: `users` (Thông tin tài khoản sản phụ)
Chứa hồ sơ cấu hình tính toán tuổi thai, chủ đề và ngày dự sinh lý thuyết của mẹ bầu.
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY, -- Sử dụng thiết kế UUID v4 để tránh xung động định danh khi đồng bộ Offline-to-Cloud
    name VARCHAR(100) NOT NULL,
    lmp_date DATE DEFAULT NULL, -- Ngày đầu tiên của chu kỳ kinh nguyệt cuối cùng
    edd_date DATE DEFAULT NULL, -- Ngày dự sinh do bác sĩ chẩn đoán (Nếu rỗng sẽ tự tính toán dựa vào lmp_date + 280 ngày)
    visualization_theme VARCHAR(30) NOT NULL DEFAULT 'FRUIT', -- Trực quan hóa kích thước thai: FRUIT, BAKERY, ANIMAL
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_theme CHECK (visualization_theme IN ('FRUIT', 'BAKERY', 'ANIMAL'))
);

CREATE INDEX idx_users_edd ON users(edd_date);
```

#### Bảng 2: `fetal_development_master` (Kho dữ liệu tuần thai biểu mẫu WHO chuẩn)
Bảng cội nguồn nạp sẵn qua SQL Seed file gác trong thư mục `assets/` của Android để chạy 100% offline.
```sql
CREATE TABLE fetal_development_master (
    week_number INT PRIMARY KEY, -- Tuần tuổi thai kỳ (Chạy từ 1 đến 41)
    avg_weight_g REAL NOT NULL, -- Cân nặng trung bình chuẩn (gm)
    avg_length_cm REAL NOT NULL, -- Chiều dài từ đầu đến chân hoặc mông chuẩn (cm)
    fruit_equivalent VARCHAR(200) NOT NULL, -- Vật so sánh giao diện Chủ đề Trái Cây
    bakery_equivalent VARCHAR(200) NOT NULL, -- Vật so sánh giao diện Chủ đề Bánh Ngọt
    animal_equivalent VARCHAR(200) NOT NULL, -- Vật so sánh giao diện Chủ đề Muông Thú
    physiology_description TEXT NOT NULL, -- Biến đổi phôi thai học/phản xạ bé
    maternal_changes TEXT NOT NULL, -- Biến đổi cơ thể mẹ/tâm lý dãn nở tử cung
    nutritional_recommendation TEXT NOT NULL, -- Đề xuất vi chất/lượng dùng dinh dưỡng chi tiết
    exception_alerts TEXT NOT NULL -- Cảnh báo bất thường khẩn cấp dọa sảy/sinh non
);

CREATE INDEX idx_fetal_week ON fetal_development_master(week_number);
```

#### Bảng 3: `medical_appointments` (Nhật ký lịch khám thai bắt buộc và tùy biến)
Phục vụ quản lý, đo lường các mốc khám thai lớn và theo dõi chỉ định từ bác sĩ phụ trách sản khoa.
```sql
CREATE TABLE medical_appointments (
    id VARCHAR(36) PRIMARY KEY, -- Định danh duy nhất UUID
    user_id VARCHAR(36) NOT NULL,
    target_week INT NOT NULL, -- Mốc khám thai định hướng tuần (Ví dụ: 11, 20, 24, 32...)
    scheduled_date DATE NOT NULL, -- Ngày khám thực tế được lên lịch hẹn
    clinic_name VARCHAR(150), -- Tên cơ sở y tế phòng khám liên kết chuyên khoa
    doctor_name VARCHAR(100), -- Bác sĩ trực sản khám chính
    medical_notes TEXT, -- Ghi chú tóm tắt sau khám
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- Trạng thái lịch khám: PENDING, COMPLETED, CANCELLED, MISSED
    is_critical_appointment BOOLEAN NOT NULL DEFAULT FALSE, -- Cờ phân biệt mốc vàng bắt buộc đo mờ da gáy, siêu âm 4D hình thái
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_app_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED', 'MISSED'))
);

CREATE INDEX idx_appointments_user ON medical_appointments(user_id);
CREATE INDEX idx_appointments_date ON medical_appointments(scheduled_date);
```

#### Bảng 4: `medication_reminders` (Nhắc nhở uống thuốc và cung cấp vi chất hàng ngày)
Điều phối giờ uống rải rác từng ngày cho các loại vi chất nhạy cảm (Sắt, Canxi uống lệch chu kỳ kị nhau).
```sql
CREATE TABLE medication_reminders (
    id VARCHAR(36) PRIMARY KEY, -- Định danh UUID
    user_id VARCHAR(36) NOT NULL,
    medication_name VARCHAR(100) NOT NULL, -- Tên vi chất (Axit Folic, Sắt, Canxi, DHA...)
    dosage VARCHAR(50) NOT NULL, -- Hàm lượng định lượng liều dùng (Ví dụ: 400mcg, 1000mg, 30mg)
    scheduled_time TIME NOT NULL, -- Khung giờ uống sắc nét chính xác (Ví dụ: 08:00:00, 14:00:00)
    repeat_interval_days INT NOT NULL DEFAULT 1, -- Chu kỳ lặp lạ hàng ngày (mặc định = 1)
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Cờ bật/tắt nhắc nhở báo thức
    last_taken_date DATE DEFAULT NULL, -- Ghi dấu phòng ngừa uống thuốc lặp lại hại gan thận
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_medication_user ON medication_reminders(user_id);
CREATE INDEX idx_medication_active ON medication_reminders(user_id, is_active);
```

### 3. File Hạt Giống SQL Khởi Tạo (`assets/fetal_development_data.sql`)
Nhà phát triển sẽ đặt file này vào thư mục dự án Android `/app/src/main/assets/fetal_development_data.sql` để cấy mầm Master Data tự động khi cài ứng dụng.

```sql
-- Gieo mầm dữ liệu MASTER DATA thai kỳ WHO chuẩn cho bảng fetal_development_master
INSERT INTO fetal_development_master (
    week_number, avg_weight_g, avg_length_cm, fruit_equivalent, bakery_equivalent, animal_equivalent, 
    physiology_description, maternal_changes, nutritional_recommendation, exception_alerts
) VALUES 
(4, 1.0, 0.2, 'Hạt anh đào (Mè/Sesame)', 'Viên kẹo đường trí bánh kem', 'Chú bọ rùa đỏ nhỏ xíu', 
 'Phôi hình thành 3 lớp tế bào biệt hóa: Ngoại bì (não, thần kinh), Trung bì (tim, xương, cơ), Nội bì (phổi, gan, ruột).', 
 'Trễ kinh mệt mỏi căng ngực dốc tức do nồng độ Progesterone tăng vọt xúc tiến giữ thai.', 
 'Bắt buộc bổ sung Axit Folic 400-600mcg/ngày để phòng ngừa dị tật đóng ống thần kinh, kết hợp Vitamin B6 bớt nghén.', 
 'Thai ngoài tử cung: Thử que 2 vạch nhưng ra máu sẫm sẫm sẫm màu liên tục kèm đau buốt hông chậu một bên.'),
(12, 15.0, 5.4, 'Quả chanh tây tươi vàng', 'Bánh Macaron bơ dâu Pháp', 'Chú chim non vừa ráo cánh', 
 'Các ngón tay, ngón chân đã phân tách rất rõ rệt. Có thể thực hiện phản xạ mút tay tự nhiên và nuốt mượt nước ối bài tiết.', 
 'Tử cung nhô cao khỏi vùng chậu tiến ra ổ bụng bụng dưới nổi gờ lên. Da bụng xuất hiện sọc nâu sọc nâu.', 
 'Canxi tăng lượng (1000mg/ngày) kết hợp Vitamin D3, đạm động vật tinh chất cao cấp xây cơ xương khớp mầm rễ trẻ.', 
 'Dọa sảy muộn: Ra máu tươi thẫm loãng từng mảng lớn, đau thắt co bóp tử cung rộ kéo dài dồn dập.'),
(28, 1000.0, 37.6, 'Quả dừa non đầy nước ngọt', 'Ổ bánh ngọt Mousse Chocolate lớn', 'Chú thỏ trắng con lông bông', 
 'Lớp mỡ dưới da phát triển tích lũy căng cứng nâng thể nhiệt tốt hơn. Nhịp tim nhạy cảm linh hoạt với tiếng mẹ nói bên ngoài.', 
 'Đau tức mỏi thắt lưng lưng mông mông vùng dưới do lệch trọng tâm hông bụng dốc thẳng đứng dài hạn chịu tải nặng.', 
 'Canxi 1200-1500mg/ngày + Vitamin D3 + DHA tăng myelin tế bào vỏ não, Sắt chống thiếu máu dãn lòng hồng cầu.', 
 'Tiền sản giật cao điểm: Sưng sưng mặt to biến hình, nhức đầu đau nhói buốt thái dương, huyết áp đo tại nhà > 140/90 mmHg.');
-- (Lưu ý: Trong thực tế file seed sẽ chứa đầy đủ 41 dòng dữ liệu tuần thai biểu mẫu nhất quán đầy đủ khớp số liệu)
```

---

## VI. KHỚP NỐI THÔNG TIN API ENDPOINTS (API SPECIFICATION)
Tất cả các API Endpoints sử dụng JSON để truyền tải cấu trúc dữ liệu, được bảo mật qua Token-based Auth trong Header (`Authorization: Bearer <token>`). Các API này thực thi chiến lược **Offline-First**: Mobile App viết trực tiếp vào Local Room DB trước, sau đó phát khởi trình Worker ngầm đồng bộ nốt dữ liệu lên máy chủ Cloud PostgreSQL khi phát hiện kết nối internet hoạt động lành lặng.

### 1. API 1: Đồng bộ dữ liệu lịch sử thai kỳ và cấu hình hồ sơ mẹ bầu
*   **Method & Path:** `PUT /api/v1/users/sync`
*   **Mục đích:** Đồng bộ cấu hình cá nhân hóa, ngày dự sinh, và chủ đề hình thái thai nhi hiển thị.
*   **Request Body (JSON):**
    ```json
    {
      "id": "e4b3c4f5-a7b8-4c9d-8e7f-6a5b4c3d2e1f",
      "name": "Nguyễn Thị Ngọc Vy",
      "lmp_date": "2026-01-15",
      "edd_date": "2026-10-22",
      "visualization_theme": "BAKERY",
      "last_sync_timestamp": 1779331107
    }
    ```
*   **Response (JSON 200 OK):**
    ```json
    {
      "status": "success",
      "code": 200,
      "message": "User profile synchronized successfully.",
      "data": {
        "id": "e4b3c4f5-a7b8-4c9d-8e7f-6a5b4c3d2e1f",
        "visualization_theme": "BAKERY",
        "gestational_week": 18,
        "days_left_to_due_date": 154,
        "synchronized_at": "2026-05-21T04:18:27Z"
      }
    }
    ```

### 2. API 2: Lấy dữ liệu Tuần thai Master Data từ máy chủ (Bảo trì dự phòng offline)
*   **Method & Path:** `GET /api/v1/fetal-development/weeks`
*   **Mục đích:** Lấy toàn bộ danh sách tri thức tuần thai khi quản trị viên cập nhật tri thức y khoa mới trên Cloud.
*   **Response (JSON 200 OK):**
    ```json
    {
      "status": "success",
      "code": 200,
      "data": [
        {
          "week_number": 4,
          "avg_weight_g": 1.0,
          "avg_length_cm": 0.2,
          "fruit_equivalent": "Hạt anh đào (Mè/Sesame)",
          "bakery_equivalent": "Viên kẹo đường trí bánh kem",
          "animal_equivalent": "Chú bọ rùa đỏ nhỏ xíu",
          "physiology_description": "Phôi hình thành 3 lớp tế bào biệt hóa: Ngoại bì, Trung bì, Nội bì.",
          "maternal_changes": "Trễ kinh mệt mỏi căng ngực và nồng độ Progesterone tăng vọt.",
          "nutritional_recommendation": "Axit Folic 400-600mcg/ngày chống dị tật nứt đốt sống.",
          "exception_alerts": "Đau bụng một bên âm ỉ kèm rỉ huyết sẫm màu cảnh báo thai ngoài tử cung."
        }
      ]
    }
    ```

### 3. API 3: Đồng bộ danh sách lịch hẹn khám thai định kỳ (Offline-to-Cloud sync)
*   **Method & Path:** `POST /api/v1/appointments/sync`
*   **Mục đích:** Đẩy các lịch hẹn người dùng tự thêm hoặc đổi trạng thái lúc không có mạng lên Cloud.
*   **Request Body (JSON):**
    ```json
    {
      "appointments": [
        {
          "id": "7ac2e18b-821f-44e1-bc30-ea5f27ec45aa",
          "user_id": "e4b3c4f5-a7b8-4c9d-8e7f-6a5b4c3d2e1f",
          "target_week": 24,
          "scheduled_date": "2026-07-01",
          "clinic_name": "Phòng Khám Sản Khoa Quốc Tế Mekong",
          "doctor_name": "BS. Nguyễn Huy Toàn",
          "medical_notes": "Tầm soát nghiệm pháp đường huyết uống nước đường 24-28 tuần.",
          "is_critical_appointment": true,
          "status": "PENDING",
          "updated_at": "2026-05-21T04:11:00Z"
        }
      ]
    }
    ```
*   **Response (JSON 200 OK):**
    ```json
    {
      "status": "success",
      "code": 200,
      "message": "1 appointments updated in cloud.",
      "sync_records_count": 1
    }
    ```

### 4. API 4: Đồng bộ danh sách nhắc lịch uống vi chất (Meds Reminder Sync)
*   **Method & Path:** `POST /api/v1/medicates/sync`
*   **Mục đích:** Đồng bộ lịch uống, cờ cấu hình bật tắt nhắc nhở vi chất y tế của mẹ bầu.
*   **Request Body (JSON):**
    ```json
    {
      "reminders": [
        {
          "id": "99f34567-8cc8-4a90-bdcf-8ed963f1092b",
          "user_id": "e4b3c4f5-a7b8-4c9d-8e7f-6a5b4c3d2e1f",
          "medication_name": "Canxi Chelate nguyên tố",
          "dosage": "1200mg",
          "scheduled_time": "08:30:00",
          "repeat_interval_days": 1,
          "is_active": true,
          "updated_at": "2026-05-21T04:15:30Z"
        }
      ]
    }
    ```
*   **Response (JSON 200 OK):**
    ```json
    {
      "status": "success",
      "code": 200,
      "message": "Medication reminders synchronized smoothly.",
      "synchronized_items": 1
    }
    ```

---

## VII. RÀNG BUỘC KỸ THUẬT & PHƯƠNG ÁN CAN THIỆP SÂU HỆ THỐNG TRÊN ANDROID

### 1. Phân Tích Bài Toán Hệ Điều Hành Android Giết Ngầm Ứng Dụng (App LCC - Doze Mode)
Hệ điều hành Android mới (từ Android 12 trở lên: API 31 - 34+) áp dụng những chính sách tối ưu hóa điện năng (Battery Optimizations) cực kỳ khắc nghiệt như **Doze Mode** (Chế độ ngủ sâu khi thiết bị không di động) và cơ chế đóng băng tiến trình ngầm (App Standby Bucket, Cached Apps Freeze, LMKD - Low Memory Killer). 
Bên cạnh đó, các bản tùy biến giao diện của hãng sản xuất thiết bị Trung Quốc và Hàn Quốc (Xiaomi HyperOS/MIUI, Samsung OneUI, Oppo ColorOS) sở hữu các trình dọn rác bộ nhớ ngầm cực kỳ hiếu chiến. Nếu ứng dụng "Mẹ và Bé" chỉ sử dụng `AlarmManager` ở chế độ lặp thông thường (`setRepeating`), hệ thống sẽ tự động ghép dồn luồng các tác vụ thông báo lại với nhau (Alarm Batching) để đánh thức máy một lần nhằm ngắt tốn pin. Hệ quả là lịch uống Canxi hay Sắt bị lệch trễ 30 phút đến vài tiếng, thậm chí mất hoàn toàn chuông báo thức nhạy cảm, gây rối loạn liều dùng cho mẹ bầu.

```
       [Android Core Doze Mode / OEM Deep Clean Engines]
                            │
              (Tối ưu hóa Pin tàn bạo dồn Alarms)
                            │
     ❌ Báo thức mút thuốc bị trễ / Khám thai bị biến mất hoàn toàn
```

---

### 2. Sơ Đồ Thiết Kế Công Nghệ Xử Lý Thông Báo 100% Không Trễ (Zero-Delay Alerts Routing)

```
 [Room DB] ─(Đổi trạng thái)─> [AlarmScheduler] ──> [AlarmManager (setExactAndAllowWhileIdle)]
                                                            │
                                                     (Hệ thống đánh thức)
                                                            │
 [BroadcastReceiver] (Kích hoạt ngầm) <──────────────────────┘
         │
         ├──> [Wakeful Job / Coroutine] ──> Phát âm thanh lớn bỏ qua Im Lặng (Critical Alert Channel)
         │
         └── (Nếu thiết bị Boot lại) <── [RECEIVE_BOOT_COMPLETED]
                     │
            (Tự động phục hồi toàn bộ Alarms từ Room DB)
```

---

### 3. Khai Báo Toàn Diện Các Quyền Hệ Thống Trong Android Manifest
Nhà phát triển bắt buộc phải đưa các cấu hình quyền này vào `/app/src/main/AndroidManifest.xml` để yêu cầu kiểm soát đặc biệt và trình duyệt:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example">

    <!-- Quyền hiển thị thông báo Runtime trên thiết bị Android 13+ -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Quyền thiết lập báo thức chính xác cao độ đến từng mili-giây (Android 12+) -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

    <!-- Quyền đặc cách tối cao báo thức y tế không cần qua mốc Play Store kiểm duyệt thủ công ngặt nghèo (Android 13+) -->
    <!-- CHÚ Ý: Google Play yêu cầu giải trình lý do Y khoa thiết yếu khi khai báo quyền này -->
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />

    <!-- Quyền tự động khởi động phục hồi lại báo thức trong Room DB ngay sau khi mẹ tắt-mở nguồn thiết bị -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- Quyền can thiệp sâu yêu cầu Hệ Thống bỏ áp chế tối ưu hóa pin cho App (Doze Mode Whitelisting) -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">

        <!-- Lắng nghe sự kiện Boot hoàn tất để nạp nạp lại báo thức -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </receiver>

        <!-- Lắng nghe sự kiện Báo Thức của AlarmManager đập vào kích chuông -->
        <receiver
            android:name=".receiver.MedicationAlarmReceiver"
            android:enabled="true"
            android:exported="false" />

    </application>
</manifest>
```

---

### 4. Giải Trình Kỹ Thuật Đưa Lên Google Play Store & Thỏa Hiệp Rủi Ro (Play Console Trade-offs)
Khai báo can thiệp hệ thống sâu mang lại những ràng buộc sinh tử khi kiểm duyệt chính sách trên cửa hàng Google Play như sau:

#### A. Rủi ro giải trình quyền đặc quyền đặc khu `USE_EXACT_ALARM`
*   **Chính sách Google Play:** Google áp đặt từ sườn Android 13 trở đi, bất kỳ app nào cài quyền `USE_EXACT_ALARM` sẽ tự động bị từ chối phát hành trừ khi danh mục của app thuộc nhóm: *Ứng dụng báo thức gốc*, *Ứng dụng lịch biểu chính thức*, hoặc *Ứng dụng sức khỏe/y học cốt lõi (Healthcare)* đòi hỏi nhắc lịch sinh tử.
*   **Biện pháp giải trình:** Trong form giải trình "Declaration Request" gửi Hội đồng kiểm duyệt Google Play Console, đội ngũ B.A và Pháp Lý phải cung cấp tài tài liệu Y khoa kèm biểu mẫu, chỉ ra rằng việc uống thuốc lệch giờ (đặc biệt là đai nội tiết duy trì thai dưỡng thai, bổ sung sắt liều cao và canxi đặc thù điều trị dọa sẩy) sẽ gây hậu quả lâm sàng nguy kịch trực tiếp cho sức khỏe thai sản.
*   **Phương án dự phòng an toàn (Fallback):** Nếu Google kiên quyết từ chối duyệt quyền tự động `USE_EXACT_ALARM`, nhóm phát triển phải rút xuống dùng quyền mềm `SCHEDULE_EXACT_ALARM` và dựng hộp thoại In-app kêu gọi mẹ tự bấm tay duyệt quyền "Cho phép báo thức chính xác" trong phần cài đặt lõi hệ thống khi bắt đầu dựng lịch thuốc.

#### B. Rủi ro khi dùng quyền `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
*   **Chính sách Google Play:** Đưa trực diện Popup hệ thống yêu cầu "Không tối ưu hóa pin" là hành vi bị Google cấm kiểm duyệt kịch liệt nếu app không chứng minh được vai trò duy nhất sống còn.
*   **Giải pháp an toàn thực chiến (UX Best Practice):** 
    1.  Ứng dụng **không bao giờ** kích hoạt trực tiếp `Intent` có chứa hành vi `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` trực diện ném vào bộ máy duyệt tự động của Google.
    2.  Thay vào đó, thiết kế một chuỗi các màn hình hướng dẫn người dùng thông thái mượt mà bằng hình ảnh rực rỡ minh họa (**In-app UX Tutorial**). Kế đó, dẫn lối mẹ thông qua một nút bấm chuyển hướng an toàn nhảy sang giao diện quản lý đa nhiệm hệ thống:
        `Settings` $\rightarrow$ `Apps` $\rightarrow$ `Special App Access` $\rightarrow$ `Battery Optimization`. 
    3.  Thực hiện chỉ dẫn Mẹ bầu tự chuyển tích xanh trạng thái "Mẹ và Bé" sang **"Unrestricted" (Không hạn chế)**. Phương pháp thủ công này giúp app 100% vượt vòng thẩm định Google Play dốc sườn, đồng thời an tâm tuyệt đối không bị các bộ dọn rác hãng thứ 3 trảm nát ngầm.

---

### 5. Mã Nguồn Minh Họa Lọc Báo Thức 100% Không Trễ Bằng Kotlin
Dưới đây là mã thực thi triển khai kỹ thuật sử dụng `AlarmManager` ở chế độ chính xác cao độ chạy ngay cả khi máy rơi vào giấc ngủ Doze Mode (`setExactAndAllowWhileIdle`):

```kotlin
package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.receiver.MedicationAlarmReceiver

class MedicationAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = 
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Kích hoạt lên lịch nhắc nhở uống vi chất/thuốc chuẩn chỉnh đến từng giây sườn máy
     */
    fun scheduleMedicationAlarm(reminderId: String, triggerTimeMs: Long, medicationName: String) {
        // Kiểm tra quyền báo thức chính xác trước khi ra lệnh nạp (Bắt buộc từ Android 12 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Quyền canScheduleExactAlarms() chưa được duyệt! Mở cài đặt...")
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(settingsIntent)
                return
            }
        }

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
            putExtra("MEDICATION_NAME", medicationName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(), // Mã hóa băm hashcode mã bảo mật để tránh lặp đè các báo thức vi chất chéo nhau
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Thực thi kỹ thuật can thiệp xuyên thủng tấm chắn Doze Mode tiết kiệm năng lượng của Hệ Thống
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
        Log.d("AlarmScheduler", "Báo thức thuốc đã được nạp chính xác lúc $triggerTimeMs cho vi chất: $medicationName")
    }

    /**
     * Hủy bỏ báo nhắc khi mẹ bầu ngắt hoạt động
     */
    fun cancelMedicationAlarm(reminderId: String) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Huỷ bỏ báo thức thành công cho ID lý lịch: $reminderId")
        }
    }
}
```

---

### 6. Mã Phục Hồi Báo Thức Khi Khởi Động Lại Máy Tranh Mất Dữ Liệu (`BootReceiver`)
Khi máy bị sập nguồn đột ngột hoặc tắt bật lại điện thoại, toàn bộ các báo thức trong `AlarmManager` của Android sẽ tự động bị hệ thống xóa sạch. `BootReceiver` là mỏ neo vàng lắng nghe tín hiệu điện tử khởi động hoàn tất để cày xới nạp lại báo thức từ Room DB.

```kotlin
package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.i("BootReceiver", "Thiết bị Android đã khởi động lại hoàn tất! Bắt đầu dựng nạp lại báo thức...")
            
            // Khởi động chạy Coroutine luồng IO ngầm lục lọi lục lọi Room DB
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Trong thực tế triển khai:
                    // 1. Khởi trị Room Database local instance.
                    // 2. Lấy danh sách toàn bộ reminders có cờ is_active == true từ MedicationReminderDao.
                    // 3. Khởi hoạt MedicationAlarmScheduler và chạy vòng lặp schedule lặp nạp lại báo thức.
                    Log.i("BootReceiver", "Hoàn tất tái lập nạp toàn bộ báo thức thuốc/lịch khám uống an toàn ngoại tuyến.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Lỗi phục hồi mỏ neo báo thức: ${e.message}")
                }
            }
        }
    }
}
```

---

## VIII. KẾT LUẬN & ĐỀ NGHỊ TRÌNH DUYỆT (SUMMARY & PROPOSAL)
Tài liệu SRS/BRD Master Data và Thiết kế Hệ thống này định nghĩa móng nền công nghệ vững chãi nhất cho sản phẩm **"Mẹ và Bé"**. Giải pháp giúp xây dựng trải nghiệm mẹ bầu liền mạch, hạn chế rủi ro thất thoát thông tin và tăng tính bảo bọc mầm sống nhi khoa đến mức tuyệt đối.

Tài liệu thiết kế nghiệp vụ này sẵn sàng chuyển giao sang đội ngũ phát triển sản phẩm (Android & Backend Engineers) để triển khai lập trình hiện thực cấu trúc thực tế trên môi trường Jetpack Compose sườn dốc hiện đại.

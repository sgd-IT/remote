import pdfplumber
pdf = pdfplumber.open('四信智慧消防云平台-设备指令API_V1.0.pdf')
for p in [8, 9, 10]:
    print(f'--- Page {p+1} ---')
    text = pdf.pages[p].extract_text()
    print(text)
    print()

# ETL Pipeline: Electric Vehicle Population Data

## สารบัญ

1. [ภาพรวมของโปรเจกต์](#1-ภาพรวมของโปรเจกต์)
2. [Dataset ที่ใช้](#2-dataset-ที่ใช้)
3. [Technology Stack](#3-technology-stack)
4. [โครงสร้างโปรเจกต์](#4-โครงสร้างโปรเจกต์)
5. [โครงสร้างข้อมูลและเครื่องมือ FP ที่ใช้](#5-โครงสร้างข้อมูลและเครื่องมือ-fp-ที่ใช้)
6. [การจัดการ Error](#6-การจัดการ-error)
7. [Business Logic ทั้ง 3 ข้อ](#7-business-logic-ทั้ง-3-ข้อ)
8. [การแปลงจาก Sequential เป็น Parallel](#8-การแปลงจาก-sequential-เป็น-parallel)
9. [ตารางเปรียบเทียบเวลา](#9-ตารางเปรียบเทียบเวลา)
10. [Overhead Discussion](#10-overhead-discussion)
11. [วิธีรันโปรเจกต์](#11-วิธีรันโปรเจกต์)

---

## 1. ภาพรวมของโปรเจกต์

โปรเจกต์นี้เป็น **ETL Pipeline** (Extract, Transform, Load) ที่เขียนด้วย **Scala 3** โดยใช้แนวคิด **Functional Programming (FP)** ทั้งหมด

ทำหน้าที่ประมวลผลข้อมูลรถยนต์ไฟฟ้า (Electric Vehicle) จากไฟล์ CSV ขนาด 135,000+ แถว โดย:

- **Extract** : อ่านไฟล์ CSV พร้อมตรวจจับและจัดการข้อมูลที่ผิดพลาด
- **Transform** : กรอง แปลง และคำนวณสถิติจากข้อมูล
- **Load** : แสดงผลลัพธ์ออกทาง console

นอกจากนี้ยังมีการเปรียบเทียบประสิทธิภาพระหว่าง **Sequential** กับ **Parallel Processing**

---

## 2. Dataset ที่ใช้

**ชื่อ:** Electric Vehicle Population Data

**แหล่งที่มา:** [Data.gov](https://www.kaggle.com/datasets/rajkumarpandey02/electric-vehicle-population-data/data)

**รายละเอียด:** ข้อมูลรถยนต์ไฟฟ้าที่จดทะเบียนในรัฐ Washington, USA

| รายการ | รายละเอียด |
|--------|-----------|
| จำนวนแถว | ~135,000 แถว |
| จำนวนคอลัมน์ | 17 คอลัมน์ |
| รูปแบบไฟล์ | CSV |

**คอลัมน์ที่ใช้ในโปรเจกต์:**

| คอลัมน์ | ความหมาย | ตัวอย่าง |
|---------|----------|---------|
| VIN (1-10) | เลขตัวถังรถ 10 หลัก | 5YJ3E1EA0K |
| County | เขต | Thurston |
| City | เมือง | Tumwater |
| State | รัฐ | WA |
| Postal Code | รหัสไปรษณีย์ | 98512 |
| Model Year | ปีรุ่นรถ | 2019 |
| Make | ยี่ห้อ | TESLA |
| Model | รุ่น | MODEL 3 |
| Electric Vehicle Type | ประเภท EV | Battery Electric Vehicle (BEV) |
| Electric Range | ระยะทางไฟฟ้า (ไมล์) | 220 |
| Base MSRP | ราคาฐาน (ดอลลาร์) | 0 |

---

## 3. Technology Stack

| เครื่องมือ | เวอร์ชัน | ใช้ทำอะไร |
|-----------|---------|----------|
| Scala | 3.8.2 | ภาษาหลักในการเขียนโปรแกรม |
| sbt | - | Build tool สำหรับ compile และ run |
| scala-parallel-collections | 1.0.4 | Library สำหรับ Parallel Processing |
| munit | 1.0.0 | Library สำหรับ Testing |
| JVM (Java) | 11+ | Runtime environment |

**เหตุผลที่เลือก Scala:**
- รองรับ Functional Programming ได้ดีเยี่ยม (pure function, immutable data, pattern matching)
- มี Parallel Collection ที่ใช้งานง่าย แค่เติม `.par`
- มี `Either`, `Try`, `Option` สำหรับจัดการ Error แบบ FP
- ทำงานบน JVM ที่มีประสิทธิภาพสูง

---

## 4. โครงสร้างโปรเจกต์

```
etl-pipeline/
├── build.sbt                                 # ตั้งค่าโปรเจกต์และ dependencies
├── Electric_Vehicle_Population_Data.csv      # ไฟล์ข้อมูล
├── src/
│   └── main/
│       └── scala/
│           ├── Models.scala              # โครงสร้างข้อมูล (case class)
│           ├── Extract.scala             # อ่าน CSV + Error Handling + Validation
│           ├── Transform.scala           # Business Logic แบบ Sequential
│           ├── ParallelTransform.scala    # Business Logic แบบ Parallel
│           ├── Benchmark.scala           # จับเวลาเปรียบเทียบ
│           └── Main.scala                # จุดเริ่มต้นโปรแกรม
└── README.md
```

---

## 5. โครงสร้างข้อมูลและเครื่องมือ FP ที่ใช้

### 5.1 Case Class : โครงสร้างข้อมูลแบบ Immutable

```scala
case class Vehicle(
  vin: String,
  county: String,
  city: String,
  state: String,
  postalCode: String,
  modelYear: Int,
  make: String,
  model: String,
  evType: String,
  electricRange: Int,
  baseMSRP: Int
)
```

**ทำไมถึงใช้ case class:**
- เป็น **immutable** สร้างแล้วเปลี่ยนค่าไม่ได้ ป้องกัน bug จากการแก้ข้อมูลโดยไม่ตั้งใจ
- ใช้ได้กับ **pattern matching** โดยอัตโนมัติ
- มี equals, hashCode, toString ให้อัตโนมัติ
- เป็นหัวใจของการเขียน FP ใน Scala

### 5.2 Either : จัดการ Error แบบ FP

```
Either[String, Vehicle]
  ├── Right(Vehicle(...))    อ่านสำเร็จ
  └── Left("error message")  อ่านไม่สำเร็จ พร้อมบอกเหตุผล
```

**ทำไมถึงใช้ Either แทน try-catch:**
- เป็น **pure function** ไม่โยน exception ทำให้ทดสอบได้ง่าย
- บังคับให้จัดการ error อย่างชัดเจน ไม่มีทางลืม
- ใช้ for-yield chain หลาย Either ได้สวยงาม

### 5.3 เครื่องมือ FP หลักที่ใช้

| เครื่องมือ FP | ใช้ตรงไหน | ทำอะไร |
|-------------|----------|--------|
| map | ทุกฟังก์ชัน | แปลงข้อมูลทุกตัวใน List โดยไม่แก้ List เดิม |
| filter | avgRangeByModel | กรองเอาเฉพาะข้อมูลที่ต้องการ |
| groupBy | ทุก Business Logic | จับกลุ่มข้อมูลตาม key ที่กำหนด |
| foldLeft | avgRangeByModel | รวมค่าทีละตัว เช่น บวกระยะทางทั้งหมด |
| partition | validatePostalCity | แบ่ง List เป็น 2 กลุ่ม (ตรง/ไม่ตรง) |
| collect | readCSV | เลือกเก็บเฉพาะ Right หรือ Left |
| pattern matching | classifyAge, parseLine | ตัดสินใจตามเงื่อนไขแบบ FP |
| for-yield | parseLine | chain หลาย Either ให้ทำงานต่อเนื่อง |

---

## 6. การจัดการ Error

### 6.1 Error Handling ตอนอ่านไฟล์ (Extract)

เมื่ออ่าน CSV แต่ละแถว ระบบจะตรวจสอบ:

1. **จำนวนคอลัมน์** : ถ้าไม่ครบ 17 คอลัมน์ จะ return Left("Row X: not enough columns")
2. **ชนิดข้อมูล** : ถ้า Model Year, Electric Range, หรือ Base MSRP ไม่ใช่ตัวเลข จะ return Left("Row X: field Y is not a number")
3. **ค่าว่าง** : ถ้าฟิลด์ตัวเลขเป็นค่าว่าง จะใส่ค่า 0 แทน (ถือเป็นค่า default ที่ยอมรับได้)

ทั้งหมดนี้ใช้ Either ทำให้โปรแกรม **ไม่หยุดทำงาน** เมื่อเจอข้อมูลผิดพลาด แต่จะเก็บ error ไว้รายงานทีหลัง

```scala
def parseLine(line: String, lineNumber: Int): Either[String, Vehicle]
// Right(Vehicle(...))  สำเร็จ
// Left("error msg")    ล้มเหลว แต่โปรแกรมทำงานต่อได้
```

### 6.2 Data Validation: Postal Code vs City

นอกจากตรวจ error แล้ว ยังมีการ **validate ความสอดคล้อง** ของข้อมูล:

**วิธีการ:**
1. สร้าง mapping จากข้อมูลเอง : ดูว่า Postal Code แต่ละตัว City ไหนเจอบ่อยสุด (majority)
2. ตรวจแต่ละแถว : ถ้า City ไม่ตรงกับ majority จะ flag ว่า "Mismatched"

**FP ที่ใช้:** groupBy + maxBy (หา majority) และ partition (แบ่งกลุ่ม matched/mismatched)

**ตัวอย่างผลลัพธ์:**

```
Matched   : 130,xxx rows
Mismatched: 4,xxx rows

Sample mismatches:
  PostalCode 98004: found Clyde Hill, expected Bellevue
  PostalCode 98001: found Federal Way, expected Auburn
```

**หมายเหตุ:** ข้อมูลที่ Mismatched ไม่จำเป็นต้องผิดเสมอไป เพราะ 1 Postal Code สามารถครอบคลุมหลายเมืองได้ แต่การ flag ไว้ช่วยให้ตรวจสอบคุณภาพข้อมูลได้

---

## 7. Business Logic ทั้ง 3 ข้อ

### 7.1 จัดช่วงรถเก่า-ใหม่ (Vehicle Age Classification)

**วัตถุประสงค์:** จัดกลุ่มรถตามปีรุ่นว่าเป็นรถใหม่ กลาง เก่า หรือเก่ามาก

**FP ที่ใช้:** Pattern Matching

```scala
def classifyAge(year: Int): String = year match {
  case y if y >= 2020 => "New (2020+)"
  case y if y >= 2015 => "Mid (2015-2019)"
  case y if y >= 2010 => "Old (2010-2014)"
  case _              => "Very Old (<2010)"
}
```

**ทำไมใช้ Pattern Matching:**
- อ่านง่าย เห็นเงื่อนไขทุกกรณีชัดเจน
- Compiler ช่วยเตือนถ้าลืมครอบคลุมบาง case
- เป็น pure function รับ Int คืน String เสมอ ไม่มีผลข้างเคียง

**ตัวอย่างผลลัพธ์:**

| กลุ่มอายุ | จำนวน |
|-----------|-------|
| New (2020+) | ~80,000 คัน |
| Mid (2015-2019) | ~28,000 คัน |
| Old (2010-2014) | ~15,000 คัน |
| Very Old (<2010) | ~10,000 คัน |

---

### 7.2 ระยะทางเฉลี่ยแต่ละรุ่น (Average Range by Model)

**วัตถุประสงค์:** คำนวณระยะทางไฟฟ้าเฉลี่ยของรถแต่ละรุ่น

**FP ที่ใช้:** filter, groupBy, foldLeft

```scala
def avgRangeByModel(vehicles: List[Vehicle]): Map[String, Double] = {
  vehicles
    .filter(_.electricRange > 0)
    .groupBy(v => s"${v.make} ${v.model}")
    .map { case (model, cars) =>
      val totalRange = cars.foldLeft(0)(_ + _.electricRange)
      val avg = totalRange.toDouble / cars.length
      (model, avg)
    }
}
```

**อธิบาย foldLeft:**

`foldLeft(0)(_ + _.electricRange)` ทำงานดังนี้:

```
เริ่มต้น: 0
+ รถคันที่ 1 (range 220)  =  220
+ รถคันที่ 2 (range 266)  =  486
+ รถคันที่ 3 (range 322)  =  808
ผลลัพธ์: 808 (นำไปหารจำนวนรถเพื่อได้ค่าเฉลี่ย)
```

---

### 7.3 สัดส่วน BEV vs PHEV ตามยี่ห้อ

**วัตถุประสงค์:** ดูว่ารถแต่ละยี่ห้อนิยมใช้ BEV (ไฟฟ้าล้วน) หรือ PHEV (ปลั๊กอินไฮบริด) มากกว่า

**คำอธิบาย:**
- **BEV** (Battery Electric Vehicle) = รถไฟฟ้าล้วน ใช้แบตเตอรี่อย่างเดียว เช่น Tesla
- **PHEV** (Plug-in Hybrid Electric Vehicle) = รถไฟฟ้า+น้ำมัน ชาร์จไฟได้ เช่น Toyota Prius Prime

**FP ที่ใช้:** groupBy ซ้อน 2 ชั้น + คำนวณสัดส่วน

```scala
vehicles
  .groupBy(_.make)           // ชั้นที่ 1: จับกลุ่มตามยี่ห้อ
  .map { case (make, cars) =>
    cars.groupBy(...)         // ชั้นที่ 2: จับกลุ่มตามประเภท BEV/PHEV
      .map { ... percentage } // คำนวณเปอร์เซ็นต์
  }
```

**ตัวอย่างผลลัพธ์:**

| ยี่ห้อ | BEV | PHEV |
|--------|-----|------|
| TESLA | 100.00% | 0.00% |
| NISSAN | 100.00% | 0.00% |
| BMW | ~45% | ~55% |
| TOYOTA | ~15% | ~85% |

---

## 8. การแปลงจาก Sequential เป็น Parallel

### Sequential (ทำทีละอย่าง)

```
Business Logic 1 จบ แล้วค่อย Business Logic 2 จบ แล้วค่อย Business Logic 3 จบ
```

ใช้โค้ดจาก Transform.scala ที่ทำงานบน List ธรรมดา

### Parallel (ทำพร้อมกัน)

ใช้ 2 เทคนิคร่วมกัน:

#### เทคนิคที่ 1: Parallel Collection (.par)

เปลี่ยนจาก List เป็น ParVector แค่เติม `.par`:

```scala
// Sequential
vehicles.map(v => classifyAge(v.modelYear)).groupBy(identity)

// Parallel : แค่เติม .par ข้างหน้า
vehicles.par.map(v => classifyAge(v.modelYear)).groupBy(identity).seq.toMap
```

`.par` จะแบ่งข้อมูลให้ CPU หลาย core ทำงานพร้อมกันอัตโนมัติ แล้ว `.seq.toMap` แปลงผลลัพธ์กลับเป็น collection ธรรมดา

**ข้อดีของ Parallel Collection:**
- แก้โค้ดน้อยมาก แค่เติม `.par` กับ `.seq`
- ไม่ต้องจัดการ thread เอง
- ทำได้เพราะ function เป็น pure function (ไม่มี shared state)

#### เทคนิคที่ 2: Future (รัน 3 Logic พร้อมกัน)

```scala
val f1 = Future { countByAgeGroup(vehicles) }    // thread 1
val f2 = Future { avgRangeByModel(vehicles) }     // thread 2
val f3 = Future { bevVsPhevByMake(vehicles) }     // thread 3

// รอผลทั้ง 3 ด้วย for-yield
val combined = for { r1 <- f1; r2 <- f2; r3 <- f3 } yield (r1, r2, r3)
```

**ทำไม pure function ถึงทำ Parallel ได้ง่าย:**
- ไม่มี `var` (ตัวแปรที่เปลี่ยนค่าได้)
- ไม่มี shared state ระหว่าง function
- แต่ละ function ทำงานอิสระ ไม่ต้อง lock ข้อมูล

---

## 9. ตารางเปรียบเทียบเวลา

> **หมายเหตุ:** ตัวเลขด้านล่างเป็นตัวอย่าง ผลจริงขึ้นอยู่กับสเปคเครื่อง ให้แทนที่ด้วยผลรันจริงของคุณ

| Task | Sequential (ms) | Parallel (ms) | Speedup | หมายเหตุ |
|------|----------------|---------------|---------|---------|
| Age Group | 45 | 25 | 1.80x faster | ข้อมูลเยอะ parallel คุ้ม |
| Avg Range | 50 | 55 | 0.91x slower | filter ลดข้อมูลลง overhead ไม่คุ้ม |
| BEV vs PHEV | 85 | 40 | 2.13x faster | groupBy ซ้อนหนัก parallel คุ้ม |
| **Total** | **180** | **120** | **1.50x faster** | **โดยรวมเร็วขึ้น** |

---

## 10. Overhead Discussion

### Parallel เร็วกว่าเสมอจริงไหม? ไม่จริง

การทำ Parallel Processing มี **overhead** (ต้นทุน) ดังนี้:

1. **Thread Creation** : การสร้างและจัดการ thread ใช้เวลา
2. **Data Splitting** : การแบ่งข้อมูลให้แต่ละ thread ใช้เวลา
3. **Result Merging** : การรวมผลลัพธ์จากหลาย thread กลับมาเป็นชิ้นเดียวใช้เวลา

### ทำไม Avg Range ถึงช้ากว่า?

1. **filter ลดข้อมูลก่อน** : กรอง electricRange > 0 ทำให้ข้อมูลที่เหลือน้อยลง งานเลยเบาจน overhead ไม่คุ้ม
2. **groupBy สร้างกลุ่มเยอะ** : จับกลุ่มตาม ยี่ห้อ + รุ่น ได้ 100+ กลุ่ม การ merge ผลจากหลาย thread กลับมาเป็น Map เดียวเป็น overhead ที่หนัก

### สรุปเงื่อนไขที่ Parallel คุ้มค่า

| เงื่อนไข | Parallel คุ้มไหม? |
|----------|-------------------|
| ข้อมูลเยอะ + งานหนัก | คุ้มมาก |
| ข้อมูลน้อย (ถูก filter ไปมาก) | ไม่คุ้ม overhead มากกว่าเวลาที่ประหยัดได้ |
| groupBy สร้างกลุ่มน้อย | คุ้ม merge ง่าย |
| groupBy สร้างกลุ่มเยอะมาก | อาจไม่คุ้ม merge หนัก |

---

## 11. วิธีรันโปรเจกต์

### สิ่งที่ต้องติดตั้ง

- sbt (Scala Build Tool)

### ขั้นตอน

```bash
# รันโปรแกรม
sbt run
```

### ผลลัพธ์ที่คาดหวัง

โปรแกรมจะแสดง:
1. จำนวนข้อมูลที่อ่านได้ และจำนวน error
2. ผล Data Validation (Postal Code vs City)
3. ผล Business Logic ทั้ง 3 ข้อ
4. ตารางเปรียบเทียบเวลา Sequential vs Parallel
5. Overhead Discussion
package nongnine

object Transform {

  // ============================================================
  // Business Logic 1: จัดช่วงรถเก่า-ใหม่ (Pattern Matching)
  // ============================================================

  // pure function: รับปี → คืนชื่อกลุ่ม
  def classifyAge(year: Int): String = year match {
    case y if y >= 2020 => "New (2020+)"
    case y if y >= 2015 => "Mid (2015-2019)"
    case y if y >= 2010 => "Old (2010-2014)"
    case _              => "Very Old (<2010)"
  }

  // นับจำนวนรถในแต่ละกลุ่มอายุ
  def countByAgeGroup(vehicles: List[Vehicle]): Map[String, Int] = {
    vehicles
      .map(v => classifyAge(v.modelYear))   // แปลงทุกคันเป็นชื่อกลุ่ม
      .groupBy(identity)                     // จับกลุ่มตามชื่อ
      .map { case (group, list) => (group, list.length) }  // นับจำนวน
  }

  // ============================================================
  // Business Logic 2: ระยะทางเฉลี่ยแต่ละรุ่น (groupBy + fold)
  // ============================================================

  def avgRangeByModel(vehicles: List[Vehicle]): Map[String, Double] = {
    vehicles
      .filter(_.electricRange > 0)           // กรองเอาเฉพาะที่มี range จริง
      .groupBy(v => s"${v.make} ${v.model}") // จับกลุ่มตาม "ยี่ห้อ รุ่น"
      .map { case (model, cars) =>
        val totalRange = cars.foldLeft(0)(_ + _.electricRange)  // รวม range ทั้งหมด
        val avg = totalRange.toDouble / cars.length              // หารจำนวนรถ
        (model, math.round(avg * 100.0) / 100.0)                // ปัดทศนิยม 2 ตำแหน่ง
      }
  }

  // ============================================================
  // Business Logic 3: สัดส่วน BEV vs PHEV ของแต่ละยี่ห้อ
  //                    (groupBy ซ้อน + คำนวณสัดส่วน)
  // ============================================================

  def bevVsPhevByMake(vehicles: List[Vehicle]): Map[String, Map[String, Double]] = {
    vehicles
      .groupBy(_.make)                       // ชั้นที่ 1: จับกลุ่มตามยี่ห้อ
      .map { case (make, cars) =>
        val total = cars.length.toDouble
        val byType = cars
          .groupBy(v =>                      // ชั้นที่ 2: จับกลุ่มตามประเภท
            if (v.evType.contains("Battery")) "BEV" else "PHEV"
          )
          .map { case (evType, typeCars) =>
            val percentage = math.round(typeCars.length / total * 10000.0) / 100.0
            (evType, percentage)              // คืนเป็น % ทศนิยม 2 ตำแหน่ง
          }
        (make, byType)
      }
  }
}
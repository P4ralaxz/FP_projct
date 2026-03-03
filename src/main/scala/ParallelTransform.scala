import scala.collection.parallel.CollectionConverters._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ParallelTransform {

  // ============================================================
  // Business Logic 1: จัดช่วงอายุรถ (Parallel Collection)
  // ============================================================
  def countByAgeGroup(vehicles: List[Vehicle]): Map[String, Int] = {
    vehicles.par                              // <-- แค่เติม .par
      .map(v => Transform.classifyAge(v.modelYear))
      .groupBy(identity)
      .map { case (group, list) => (group, list.length) }
      .seq.toMap                              // <-- แปลงกลับเป็น Map ธรรมดา
  }

  // ============================================================
  // Business Logic 2: ระยะทางเฉลี่ย (Parallel Collection)
  // ============================================================
  def avgRangeByModel(vehicles: List[Vehicle]): Map[String, Double] = {
    vehicles.par
      .filter(_.electricRange > 0)
      .groupBy(v => s"${v.make} ${v.model}")
      .map { case (model, cars) =>
        val totalRange = cars.foldLeft(0)(_ + _.electricRange)
        val avg = totalRange.toDouble / cars.length
        (model, math.round(avg * 100.0) / 100.0)
      }
      .seq.toMap
  }

  // ============================================================
  // Business Logic 3: BEV vs PHEV (Parallel Collection)
  // ============================================================
  def bevVsPhevByMake(vehicles: List[Vehicle]): Map[String, Map[String, Double]] = {
    vehicles.par
      .groupBy(_.make)
      .map { case (make, cars) =>
        val total = cars.length.toDouble
        val byType = cars
          .groupBy(v =>
            if (v.evType.contains("Battery")) "BEV" else "PHEV"
          )
          .map { case (evType, typeCars) =>
            val percentage = math.round(typeCars.length / total * 10000.0) / 100.0
            (evType, percentage)
          }
          .seq.toMap
        (make, byType)
      }
      .seq.toMap
  }

  // ============================================================
  // รันทั้ง 3 Logic พร้อมกัน ด้วย Future
  // ============================================================
  def runAll(vehicles: List[Vehicle]): (Map[String, Int], Map[String, Double], Map[String, Map[String, Double]]) = {
    val f1 = Future { countByAgeGroup(vehicles) }
    val f2 = Future { avgRangeByModel(vehicles) }
    val f3 = Future { bevVsPhevByMake(vehicles) }

    // รอผลลัพธ์ทั้ง 3 ตัว
    val combined = for {
      r1 <- f1
      r2 <- f2
      r3 <- f3
    } yield (r1, r2, r3)

    Await.result(combined, 60.seconds)
  }
}
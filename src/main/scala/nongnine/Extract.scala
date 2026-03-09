package nongnine

import scala.io.Source
import scala.util.{Try, Using}

object Extract {

  // pure function: แปลง 1 บรรทัด CSV → Either[Error, Vehicle]
  def parseLine(line: String, lineNumber: Int): Either[String, Vehicle] = {
    val fields = line.split(",", -1)  // -1 เพื่อเก็บค่าว่างด้วย

    if (fields.length < 17) {
      Left(s"Row $lineNumber: not enough columns (found ${fields.length}, expected 17)")
    } else {
      for {
        modelYear    <- parseIntField(fields(5).trim, "Model Year", lineNumber)
        electricRange <- parseIntField(fields(10).trim, "Electric Range", lineNumber)
        baseMSRP     <- parseIntField(fields(11).trim, "Base MSRP", lineNumber)
      } yield Vehicle(
        vin         = fields(0).trim,
        county      = fields(1).trim,
        city        = fields(2).trim,
        state       = fields(3).trim,
        postalCode  = fields(4).trim,
        modelYear   = modelYear,
        make        = fields(6).trim,
        model       = fields(7).trim,
        evType      = fields(8).trim,
        electricRange = electricRange,
        baseMSRP    = baseMSRP
      )
    }
  }

  // helper: แปลง String → Int อย่างปลอดภัย
  private def parseIntField(value: String, fieldName: String, lineNumber: Int): Either[String, Int] = {
    if (value.isEmpty) Right(0)
    else Try(value.toInt).toEither.left.map(_ =>
      s"Row $lineNumber: $fieldName '$value' is not a number"
    )
  }

  // อ่านไฟล์ทั้งหมด → แยกเป็น (List[Error], List[Vehicle])
  def readCSV(filePath: String): (List[String], List[Vehicle]) = {
    val lines = Using(Source.fromFile(filePath)) { source =>
      source.getLines().toList
    }.getOrElse(List.empty)

    if (lines.isEmpty) {
      (List("Can't Read This File"), List.empty)
    } else {
      val results = lines.tail.zipWithIndex.map { case (line, idx) =>
        parseLine(line, idx + 2)  // +2 เพราะ header=1, index เริ่มที่ 0
      }

      val errors   = results.collect { case Left(err)     => err }
      val vehicles = results.collect { case Right(vehicle) => vehicle }

      (errors, vehicles)
    }
  }

  // Validation: เช็คว่า PostalCode กับ City ตรงกันไหม
  // Step 1: สร้าง mapping จากข้อมูล (PostalCode -> City ที่เจอบ่อยสุด)
  def buildPostalCityMap(vehicles: List[Vehicle]): Map[String, String] = {
    vehicles
      .groupBy(_.postalCode)
      .map { case (pc, cars) =>
        val majorityCity = cars
          .groupBy(_.city)
          .maxBy { case (_, list) => list.length }
          ._1
        (pc, majorityCity)
      }
  }

  // Step 2: เช็คแต่ละแถวว่าตรงกับ majority ไหม
  def validatePostalCity(
    vehicles: List[Vehicle],
    postalCityMap: Map[String, String]
  ): (List[Vehicle], List[Vehicle]) = {
    vehicles.partition { v =>
      postalCityMap.get(v.postalCode) match {
        case Some(expectedCity) => v.city == expectedCity
        case None               => true  // ไม่มีข้อมูลเปรียบเทียบ ถือว่าผ่าน
      }
    }
    // คืน (matched, mismatched)
  }
}
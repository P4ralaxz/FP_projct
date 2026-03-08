package nongnine

object Benchmark {

  // pure function: จับเวลาการทำงาน
  // รับ function ที่จะรัน → คืน (ผลลัพธ์, เวลาที่ใช้ ms)
  def measure[T](label: String)(block: => T): (T, Long) = {
    val start  = System.nanoTime()
    val result = block
    val end    = System.nanoTime()
    val timeMs = (end - start) / 1_000_000
    (result, timeMs)
  }

  // รัน Sequential ทั้ง 3 Logic แล้วจับเวลาแต่ละอัน
  def runSequential(vehicles: List[Vehicle]): Map[String, Long] = {
    val (_, t1) = measure("Age Group")      { Transform.countByAgeGroup(vehicles) }
    val (_, t2) = measure("Avg Range")      { Transform.avgRangeByModel(vehicles) }
    val (_, t3) = measure("BEV vs PHEV")    { Transform.bevVsPhevByMake(vehicles) }

    Map(
      "Age Group"   -> t1,
      "Avg Range"   -> t2,
      "BEV vs PHEV" -> t3,
      "Total"       -> (t1 + t2 + t3)
    )
  }

  // รัน Parallel ทั้ง 3 Logic แล้วจับเวลาแต่ละอัน
  def runParallel(vehicles: List[Vehicle]): Map[String, Long] = {
    val (_, t1) = measure("Age Group")      { ParallelTransform.countByAgeGroup(vehicles) }
    val (_, t2) = measure("Avg Range")      { ParallelTransform.avgRangeByModel(vehicles) }
    val (_, t3) = measure("BEV vs PHEV")    { ParallelTransform.bevVsPhevByMake(vehicles) }

    Map(
      "Age Group"   -> t1,
      "Avg Range"   -> t2,
      "BEV vs PHEV" -> t3,
      "Total"       -> (t1 + t2 + t3)
    )
  }

  // สร้างตารางเปรียบเทียบ
  def printComparison(seqTimes: Map[String, Long], parTimes: Map[String, Long]): Unit = {
    val tasks = List("Age Group", "Avg Range", "BEV vs PHEV", "Total")

    println("+----------------+-----------------+-----------------+-----------+")
    println("| Task           | Sequential (ms) | Parallel (ms)   | Speedup   |")
    println("+----------------+-----------------+-----------------+-----------+")

    tasks.foreach { task =>
      val seqT = seqTimes(task)
      val parT = parTimes(task)
      val speedup = if (parT > 0) seqT.toDouble / parT.toDouble else 0.0

      val arrow = if (speedup >= 1.0) "faster" else "slower"

      println(f"| $task%-14s | $seqT%15d | $parT%15d | ${speedup}%6.2fx $arrow%-6s |")
    }

    println("+----------------+-----------------+-----------------+-----------+")
  }
}
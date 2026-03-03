@main def run(): Unit =

  println("=== NinenoiInwza: Electric Vehicle Data ===\n")

  // ---- Extract ----
  println("Reading data...")
  val (errors, vehicles) = Extract.readCSV("Electric_Vehicle_Population_Data.csv")
  println(s"Successfully read: ${vehicles.length} rows | Errors: ${errors.length} rows\n")

  if errors.nonEmpty then
    println("Sample Errors:")
    errors.take(5).foreach(e => println(s"  $e"))
    println()

  // ---- Validation: Postal Code vs City ----
  println("--- Data Validation: Postal Code vs City ---")
  val postalCityMap = Extract.buildPostalCityMap(vehicles)
  val (matched, mismatched) = Extract.validatePostalCity(vehicles, postalCityMap)
    if (mismatched.nonEmpty) {
    println("\n  Sample mismatches:")
    mismatched.take(5).foreach { v =>
      val expected = postalCityMap.getOrElse(v.postalCode, "???")
      println(s"    PostalCode ${v.postalCode}: found '${v.city}', expected '$expected'")
    }
  }
  println()

  println(s"  Matched   : ${matched.length} rows")
  println(s"  Mismatched: ${mismatched.length} rows")
  println()
  // ---- Transform ----

  // Business Logic 1: Vehicle Age Classification
  println("--- 1. Vehicle Count by Age Group ---")
  val ageGroups = Transform.countByAgeGroup(vehicles)
  ageGroups.toList.sortBy(_._1).foreach { case (group, count) =>
    println(f"  $group%-20s : $count%,d vehicles")
  }

  // Business Logic 2: Average Range by Model (Top 10)
  println("\n--- 2. Top 10 Average Electric Range by Model ---")
  val avgRange = Transform.avgRangeByModel(vehicles)
  avgRange.toList.sortBy(-_._2).take(10).foreach { case (model, avg) =>
    println(f"  $model%-30s : $avg%.2f miles")
  }

  // Business Logic 3: BEV vs PHEV Ratio by Make (Top 10)
  println("\n--- 3. BEV vs PHEV Ratio by Make (Top 10) ---")
  val bevPhev = Transform.bevVsPhevByMake(vehicles)
  bevPhev.toList
    .sortBy { case (_, types) => -types.values.sum }
    .take(10)
    .foreach { case (make, types) =>
      val bev  = types.getOrElse("BEV", 0.0)
      val phev = types.getOrElse("PHEV", 0.0)
      println(f"  $make%-15s : BEV $bev%6.2f%% | PHEV $phev%6.2f%%")
    }



  // ---- Benchmark: Sequential vs Parallel ----
  println("\n=== Performance Comparison: Sequential vs Parallel ===\n")

  // Warm-up run (JVM needs to warm up for fair comparison)
  println("Warming up JVM...")
  Transform.countByAgeGroup(vehicles)
  ParallelTransform.countByAgeGroup(vehicles)
  println("Warm-up done.\n")

  // Run benchmark
  val seqTimes = Benchmark.runSequential(vehicles)
  val parTimes = Benchmark.runParallel(vehicles)

  Benchmark.printComparison(seqTimes, parTimes)

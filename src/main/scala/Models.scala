// case class = โครงสร้างข้อมูลแบบ FP (immutable, ไม่เปลี่ยนค่าได้)
case class Vehicle(
  vin: String,
  county: String,
  city: String,
  state: String,
  postalCode: String,
  modelYear: Int,
  make: String,
  model: String,
  evType: String,        // "Battery Electric Vehicle (BEV)" หรือ "Plug-in Hybrid..."
  electricRange: Int,
  baseMSRP: Int
)
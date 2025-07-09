import org.scalatest.funsuite.AnyFunSuite
import pcd.ass01.Model.{P2d, V2d}

class ModelTestSuite extends AnyFunSuite:

  test("P2d should be created with correct coordinates") {
    val point = P2d(10.5, 20.3)
    assert(point.x == 10.5)
    assert(point.y == 20.3)
  }

  test("P2d should calculate distance correctly") {
    val p1 = P2d(0, 0)
    val p2 = P2d(3, 4)
    val distance = p1.distance(p2)
    assert(distance == 5.0) // 3-4-5 triangle
    
    val p3 = P2d(1, 1)
    val p4 = P2d(1, 1)
    assert(p3.distance(p4) == 0.0) // Same point
  }

  test("P2d should sum with V2d correctly") {
    val point = P2d(10, 20)
    val vector = V2d(5, -3)
    val result = point.sum(vector)
    assert(result.x == 15.0)
    assert(result.y == 17.0)
  }

  test("P2d should subtract another P2d to get V2d") {
    val p1 = P2d(10, 15)
    val p2 = P2d(4, 5)
    val result = p1.sub(p2)
    assert(result.x == 6.0)
    assert(result.y == 10.0)
  }

  test("P2d toString should format correctly") {
    val point = P2d(1.5, 2.7)
    assert(point.toString == "P2d(1.5,2.7)")
  }

  test("V2d should be created with correct components") {
    val vector = V2d(3.5, -2.1)
    assert(vector.x == 3.5)
    assert(vector.y == -2.1)
  }

  test("V2d should sum correctly") {
    val v1 = V2d(2, 3)
    val v2 = V2d(4, -1)
    val result = v1.sum(v2)
    assert(result.x == 6.0)
    assert(result.y == 2.0)
  }

  test("V2d should calculate absolute value (magnitude) correctly") {
    val v1 = V2d(3, 4)
    assert(v1.abs == 5.0) // 3-4-5 triangle
    
    val v2 = V2d(0, 0)
    assert(v2.abs == 0.0)
    
    val v3 = V2d(-3, -4)
    assert(v3.abs == 5.0) // Should handle negative values
  }

  test("V2d should normalize correctly") {
    val v1 = V2d(3, 4)
    val normalized = v1.getNormalized
    assert(math.abs(normalized.abs - 1.0) < 0.0001) // Should have magnitude 1
    assert(math.abs(normalized.x - 0.6) < 0.0001)
    assert(math.abs(normalized.y - 0.8) < 0.0001)
  }

  test("V2d should multiply by scalar correctly") {
    val vector = V2d(2, 3)
    val scaled = vector.mul(2.5)
    assert(scaled.x == 5.0)
    assert(scaled.y == 7.5)
    
    val negativeScale = vector.mul(-1)
    assert(negativeScale.x == -2.0)
    assert(negativeScale.y == -3.0)
  }

  test("V2d toString should format correctly") {
    val vector = V2d(1.5, -2.7)
    assert(vector.toString == "V2d(1.5,-2.7)")
  }

  test("V2d zero vector edge cases") {
    val zero = V2d(0, 0)
    assert(zero.abs == 0.0)
    
    // Normalizing zero vector should handle division by zero
    // This might throw an exception or return NaN - test the actual behavior
    val normalizedZero = zero.getNormalized
    assert(normalizedZero.x.isNaN)
    assert(normalizedZero.y.isNaN)
  }

  test("P2d and V2d interaction") {
    val point = P2d(5, 10)
    val vector = V2d(2, -3)
    
    // Test round trip: point + vector - point = vector
    val newPoint = point.sum(vector)
    val resultVector = newPoint.sub(point)
    
    assert(math.abs(resultVector.x - vector.x) < 0.0001)
    assert(math.abs(resultVector.y - vector.y) < 0.0001)
  }

  test("Vector operations maintain precision") {
    val v1 = V2d(0.1, 0.2)
    val v2 = V2d(0.3, 0.4)
    val sum = v1.sum(v2)
    
    // Test that floating point operations work as expected
    assert(math.abs(sum.x - 0.4) < 0.0001)
    assert(math.abs(sum.y - 0.6) < 0.0001)
  }

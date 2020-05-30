@file:JsQualifier("THREE")

package three

open external class Blending
external object NoBlending : Blending
external object NormalBlending : Blending
external object AdditiveBlending : Blending
external object SubtractiveBlending : Blending
external object MultiplyBlending : Blending
external object CustomBlending : Blending

open external class Side
external object FrontSide: Side
external object BackSide: Side
external object DoubleSide: Side

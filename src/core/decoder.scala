/*
    Spectacular, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package spectacular

import rudiments.*
import fulminate.*
import contingency.*
import anticipation.*
import inimitable.*
import digression.*

import language.experimental.pureFunctions

case class NumberError(text: Text, specializable: Specializable)
extends Error(m"$text is not a valid ${specializable.show}")

case class EnumCaseError(text: Text) extends Error(m"$text is not a valid enumeration case")

object Decoder:
  given int(using number: Errant[NumberError]): Decoder[Int] = text =>
    try Integer.parseInt(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Int))(0)

  given fqcn(using fqcn: Errant[FqcnError]): Decoder[Fqcn] = Fqcn(_)
  given uuid(using uuid: Errant[UuidError]): Decoder[Uuid] = Uuid.parse(_)

  given byte(using number: Errant[NumberError]): Decoder[Byte] = text =>
    val int = try Integer.parseInt(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Byte))(0)

    if int < Byte.MinValue || int > Byte.MaxValue then raise(NumberError(text, Byte))(0.toByte)
    else int.toByte

  given short(using number: Errant[NumberError]): Decoder[Short] = text =>
    val int = try Integer.parseInt(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Short))(0)

    if int < Short.MinValue || int > Short.MaxValue then raise(NumberError(text, Short))(0.toShort)
    else int.toShort

  given long(using number: Errant[NumberError]): Decoder[Long] = text =>
    try java.lang.Long.parseLong(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Long))(0L)

  given double(using number: Errant[NumberError]): Decoder[Double] = text =>
    try java.lang.Double.parseDouble(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Double))(0.0)

  given float(using number: Errant[NumberError]): Decoder[Float] = text =>
    try java.lang.Float.parseFloat(text.s) catch case _: NumberFormatException =>
      raise(NumberError(text, Float))(0.0F)

  given char: Decoder[Char] = _.s(0)
  given text: Decoder[Text] = identity(_)
  given string: Decoder[String] = _.s
  given pid(using number: Errant[NumberError]): Decoder[Pid] = long.map(Pid(_))

  //given enumDecoder[EnumType <: reflect.Enum & Product](using Mirror.SumOf[EnumType], Errant[EnumCaseError]): Decoder[EnumType] = text =>
  //  Unapply.valueOf[EnumType].unapply(text).getOrElse(abort(EnumCaseError(text)))

@capability
trait Decoder[+ValueType] extends Unapply[Text, ValueType]:
  def unapply(text: Text): Option[ValueType] = try Some(decode(text)) catch case error: Exception => None
  def decode(text: Text): ValueType
  def map[ValueType2](lambda: ValueType => ValueType2): Decoder[ValueType2] = text => lambda(decode(text))

object Encoder:
  given int: Encoder[Int] = _.toString.tt
  given double: Encoder[Double] = _.toString.tt
  given byte: Encoder[Byte] = _.toString.tt
  given short: Encoder[Short] = _.toString.tt
  given long: Encoder[Long] = _.toString.tt
  given float: Encoder[Float] = _.toString.tt
  given text: Encoder[Text] = identity(_)
  given char: Encoder[Char] = _.toString.tt
  given uuid: Encoder[Uuid] = _.text
  given pid: Encoder[Pid] = long.contramap(_.value)
  given fqcn: Encoder[Fqcn] = _.text

@capability
trait Encoder[-ValueType] extends Irrefutable[ValueType, Text]:
  def unapply(value: ValueType): Text = encode(value)
  def encode(value: ValueType): Text
  def contramap[ValueType2](lambda: ValueType2 => ValueType): Encoder[ValueType2] = value => encode(lambda(value))

extension (text: Text)
  def decodeAs[ValueType](using decoder: Decoder[ValueType]): ValueType =
    decoder.decode(text)

extension [ValueType](value: ValueType)
  def encode(using encoder: Encoder[ValueType]): Text = encoder.encode(value)

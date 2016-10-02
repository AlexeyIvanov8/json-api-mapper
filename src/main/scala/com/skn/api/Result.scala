package com.skn.api

trait Result[T]

case class Success[T](value: T, message: String = "") extends Result[T]
case class Fail[T](message: String) extends Result[T]
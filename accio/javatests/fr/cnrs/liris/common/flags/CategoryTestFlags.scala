package fr.cnrs.liris.common.flags

case class CategoryTestFlags(
  @Flag(name = "swiss_bank_account_number", category = "undocumented")
  swissBankAccountNumber: Int = 123456789,
  @Flag(name = "student_bank_account_number", category = "one")
  studentBankAccountNumber: Int = 987654321)

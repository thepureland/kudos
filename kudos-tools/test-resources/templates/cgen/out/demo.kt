import t.T

/**
 * Fixture template for CodeGeneratorTest.
 *
 * The append-region markers sit at column 0 on purpose: PrivateContentEraser strips the marker
 * lines but not their leading indentation, so keeping them unindented leaves the erased result
 * clean on a first-time generation.
 */
class Demo {

    //region your codes 1
    //endregion your codes 1

//region append IMPARTIBLE codes 1
    val appended = "${greeting}"
//endregion append IMPARTIBLE codes 1

}

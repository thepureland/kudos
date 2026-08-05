package io.kudos.ms.sys.common.datasource.consts

/**
 * Constants of the data source module.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object SysDataSourceConsts {

    /**
     * Fixed mask returned in place of the real (encrypted) password on every admin-facing
     * response VO (`SysDataSourceRow` / `SysDataSourceDetail` / `SysDataSourceEdit`).
     *
     * On the write path, submitting this mask (or a blank value) in an update form means
     * "keep the stored password unchanged", so a round-tripped edit form never overwrites
     * the persisted ciphertext with the mask.
     */
    const val PASSWORD_MASK = "******"

}

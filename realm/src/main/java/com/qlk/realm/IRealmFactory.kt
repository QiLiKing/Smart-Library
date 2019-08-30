package com.qlk.realm

import io.realm.Realm
import io.realm.RealmModel

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-07-27.
 */
interface IRealmFactory {
    /**
     * https://realm.io/docs/java/latest/#faq-view-realms
     * It's dangerous to continue running app when a realm error occurs. So we should terminate the app.
     * @return null - database error (you can restore it here with a blocked snippet)
     *                no such table
     */
    fun open(table: Class<out RealmModel>): Realm

    fun getCopyDeep(table: Class<out RealmModel>): Int = 0
}
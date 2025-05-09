/*
 * Copyright 2023-2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.db.impl

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.db.impl.entities.LocalChangeEntity
import com.google.android.fhir.toTimeZoneString
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceDatabaseMigrationTest {

  @get:Rule
  val helper: MigrationTestHelper =
    MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ResourceDatabase::class.java)
  private val iParser = FhirContext.forR4Cached().newJsonParser()

  @Test
  @Throws(IOException::class)
  fun migrate1To2_should_not_throw_exception(): Unit = runBlocking {
    val insertedPatientJson: String =
      Patient()
        .apply {
          id = "migrate1-2-test"
          addName(
            HumanName().apply {
              addGiven("Jane")
              family = "Doe"
            },
          )
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 1).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('migrate1-2-test', 'Patient', 'migrate1-2-test', '$insertedPatientJson' );",
      )
      close()
    }

    // Open latest version of the database. Room will validate the schema
    // once all migrations execute.
    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

    val readPatientJson: String?
    migratedDatabase.let { database ->
      database.query("SELECT serializedResource FROM ResourceEntity").let {
        it.moveToFirst()
        readPatientJson = it.getString(0)
      }
    }
    migratedDatabase.close()
    assertThat(readPatientJson).isEqualTo(insertedPatientJson)
  }

  @Test
  @Throws(IOException::class)
  fun migrate2To3_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val bedNetTask: String =
      Task()
        .apply {
          id = taskId
          description = "Issue bed net"
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 2).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('bed-net-001', 'Task', 'bed-net-001', '$bedNetTask');",
      )
      close()
    }

    // Re-open the database with version 3 and provide MIGRATION_2_3 as the migration process.
    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

    val retrievedTask: String?
    migratedDatabase.let { database ->
      database.query("SELECT serializedResource FROM ResourceEntity").let {
        it.moveToFirst()
        retrievedTask = it.getString(0)
      }
    }
    migratedDatabase.close()
    assertThat(retrievedTask).isEqualTo(bedNetTask)
  }

  @Test
  @Throws(IOException::class)
  fun migrate3To4_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val bedNetTask: String =
      Task()
        .apply {
          id = taskId
          description = "Issue bed net"
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 3).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('bed-net-001', 'Task', 'bed-net-001', '$bedNetTask');",
      )
      close()
    }

    // Re-open the database with version 4 and provide MIGRATION_3_4 as the migration process.
    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

    val retrievedTask: String?
    migratedDatabase.let { database ->
      database.query("SELECT serializedResource FROM ResourceEntity").let {
        it.moveToFirst()
        retrievedTask = it.getString(0)
      }
    }
    migratedDatabase.close()
    assertThat(retrievedTask).isEqualTo(bedNetTask)
  }

  @Test
  fun migrate4To5_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val bedNetTask: String =
      Task()
        .apply {
          id = taskId
          description = "Issue bed net"
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 4).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('bed-net-001', 'Task', 'bed-net-001', '$bedNetTask');",
      )
      close()
    }

    // Re-open the database with version 5 and provide MIGRATION_4_5 as the migration process.
    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

    val retrievedTask: String?
    migratedDatabase.let { database ->
      database.query("SELECT serializedResource FROM ResourceEntity").let {
        it.moveToFirst()
        retrievedTask = it.getString(0)
      }
    }
    migratedDatabase.close()
    assertThat(retrievedTask).isEqualTo(bedNetTask)
  }

  @Test
  fun migrate5To6_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val bedNetTask: String =
      Task()
        .apply {
          id = taskId
          description = "Issue bed net"
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    // Since the migration here is to change the column type of LocalChangeEntity.timestamp from
    // string to Instant (integer). We are making sure that the data is migrated properly.
    helper.createDatabase(DB_NAME, 5).apply {
      val date = Date()
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource, lastUpdatedLocal) VALUES ('bed-net-001', 'Task', 'bed-net-001', '$bedNetTask', '${DbTypeConverters.instantToLong(date.toInstant())}' );",
      )

      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceId, timestamp, type, payload) VALUES ('Task', 'bed-net-001', '${date.toTimeZoneString()}', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.INSERT)}', '$bedNetTask'  );",
      )

      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceId, timestamp, type, payload) VALUES ('Task', 'id-corrupted-timestamp', 'date-not-good', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.INSERT)}', '$bedNetTask'  );",
      )
      close()
    }

    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 6, true, MIGRATION_5_6)

    val retrievedTask: String?
    val localChangeEntityTimeStamp: Long
    val resourceEntityLastUpdatedLocal: Long
    val localChangeEntityCorruptedTimeStamp: Long

    migratedDatabase.let { database ->
      database.query("SELECT serializedResource FROM ResourceEntity").let {
        it.moveToFirst()
        retrievedTask = it.getString(0)
      }

      resourceEntityLastUpdatedLocal =
        database.query("Select lastUpdatedLocal from ResourceEntity").let {
          it.moveToFirst()
          it.getLong(0)
        }

      database.query("SELECT timestamp FROM LocalChangeEntity").let {
        it.moveToFirst()
        localChangeEntityTimeStamp = it.getLong(0)
        it.moveToNext()
        localChangeEntityCorruptedTimeStamp = it.getLong(0)
      }
    }
    migratedDatabase.close()
    assertThat(retrievedTask).isEqualTo(bedNetTask)
    assertThat(localChangeEntityTimeStamp).isEqualTo(resourceEntityLastUpdatedLocal)
    assertThat(Instant.ofEpochMilli(localChangeEntityCorruptedTimeStamp)).isEqualTo(Instant.EPOCH)
  }

  @Test
  fun migrate6To7_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val taskResourceUuid = "e2c79e28-ed4d-4029-a12c-108d1eb5bedb"
    val bedNetTask: String =
      Task()
        .apply {
          id = taskId
          description = "Issue bed net"
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 6).apply {
      val date = Date()
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource, lastUpdatedLocal) VALUES ('$taskResourceUuid', 'Task', '$taskId', '$bedNetTask', '${DbTypeConverters.instantToLong(date.toInstant())}' );",
      )

      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceId, timestamp, type, payload) VALUES ('Task', '$taskId', '${date.toTimeZoneString()}', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.INSERT)}', '$bedNetTask'  );",
      )
      close()
    }

    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 7, true, MIGRATION_6_7)

    val retrievedTaskResourceId: String?
    val retrievedTaskResourceUuid: String?
    val localChangeResourceUuid: String?
    val localChangeResourceId: String?

    migratedDatabase.let { database ->
      database.query("SELECT resourceId, resourceUuid FROM ResourceEntity").let {
        it.moveToFirst()
        retrievedTaskResourceId = it.getString(0)
        retrievedTaskResourceUuid = String(it.getBlob(1), Charsets.UTF_8)
      }

      database.query("SELECT resourceId,resourceUuid FROM LocalChangeEntity").let {
        it.moveToFirst()
        localChangeResourceId = it.getString(0)
        localChangeResourceUuid = String(it.getBlob(1), Charsets.UTF_8)
      }
    }
    migratedDatabase.close()
    assertThat(retrievedTaskResourceUuid).isEqualTo(localChangeResourceUuid)
    assertThat(localChangeResourceId).isEqualTo(retrievedTaskResourceId)
  }

  @Test
  fun migrate7To8_should_execute_with_no_exception(): Unit = runBlocking {
    val patientId = "patient-001"
    val patientResourceUuid = "e2c79e28-ed4d-4029-a12c-108d1eb5bedb"
    val patient: String =
      Patient()
        .apply {
          id = patientId
          addName(HumanName().apply { addGiven("Brad") })
          addGeneralPractitioner(Reference("Practitioner/123"))
          managingOrganization = Reference("Organization/123")
          meta.lastUpdated = Date()
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 7).apply {
      val insertionDate = Date()
      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceUuid, resourceId, timestamp, type, payload) VALUES ('Patient', '$patientResourceUuid', '$patientId', '${insertionDate.toTimeZoneString()}', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.INSERT)}', '$patient'  );",
      )
      val updateDate = Date()
      val patch =
        "[{\"op\":\"replace\",\"path\":\"\\/generalPractitioner\\/0\\/reference\",\"value\":\"Practitioner\\/345\"}]"
      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceUuid, resourceId, timestamp, type, payload) VALUES ('Patient', '$patientResourceUuid', '$patientId', '${updateDate.toTimeZoneString()}', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.UPDATE)}', '$patch'  );",
      )
      val deleteDate = Date()
      execSQL(
        "INSERT INTO LocalChangeEntity (resourceType, resourceUuid, resourceId, timestamp, type, payload) VALUES ('Patient', '$patientResourceUuid', '$patientId', '${deleteDate.toTimeZoneString()}', '${DbTypeConverters.localChangeTypeToInt(LocalChangeEntity.Type.DELETE)}', ''  );",
      )
      close()
    }

    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 8, true, MIGRATION_7_8)

    var localChange1Id: Long
    var localChange2Id: Long

    var localChangeReferences: MutableMap<Long, MutableList<String>>

    migratedDatabase.let { database ->
      database.query("SELECT id FROM LocalChangeEntity").let {
        it.moveToFirst()
        localChange1Id = it.getLong(0)
        it.moveToNext()
        localChange2Id = it.getLong(0)
      }

      database
        .query(
          "SELECT localChangeId, resourceReferenceValue FROM LocalChangeResourceReferenceEntity",
        )
        .let {
          var continueToNextRow = it.moveToFirst()
          localChangeReferences = mutableMapOf()
          while (continueToNextRow) {
            val localChangeId = it.getLong(0)
            val referenceValue = it.getString(1)
            val existingList = localChangeReferences.getOrDefault(localChangeId, mutableListOf())
            existingList.add(referenceValue)
            localChangeReferences[localChangeId] = existingList
            continueToNextRow = it.moveToNext()
          }
        }
    }
    migratedDatabase.close()
    assertThat(localChangeReferences).containsKey(localChange1Id)
    assertThat(localChangeReferences).containsKey(localChange2Id)
    assertThat(localChangeReferences[localChange1Id]!!.size).isEqualTo(2)
    assertThat(localChangeReferences[localChange2Id]!!.size).isEqualTo(1)
    assertThat(localChangeReferences[localChange1Id]!!)
      .containsExactly("Practitioner/123", "Organization/123")
    assertThat(localChangeReferences[localChange2Id]!!).containsExactly("Practitioner/345")
  }

  @Test
  fun migrate8To9_should_execute_with_no_exception(): Unit = runBlocking {
    val taskId = "bed-net-001"
    val taskResourceUuid = "8593abf6-b8dd-44d7-a35f-1c8843bc2c45"
    val date = Date()
    val bedNetTask =
      Task()
        .apply {
          id = taskId
          status = Task.TaskStatus.READY
          meta.lastUpdated = date
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 8).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource, lastUpdatedLocal) VALUES ('$taskResourceUuid', 'Task', '$taskId', '$bedNetTask', '${DbTypeConverters.instantToLong(date.toInstant())}' );",
      )
      execSQL(
        "INSERT INTO TokenIndexEntity (resourceUuid, resourceType, index_name, index_path, index_system, index_value) VALUES ('$taskResourceUuid', 'Task', 'status', 'Task.status', 'http://hl7.org/fhir/task-status', 'ready');",
      )
      close()
    }

    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 9, true, Migration_8_9)

    val retrievedTask: String?
    migratedDatabase.let { database ->
      database
        .query(
          """
        SELECT a.serializedResource FROM ResourceEntity a
        WHERE a.resourceType = 'Task'
          AND a.resourceUuid IN (SELECT resourceUuid FROM TokenIndexEntity
            WHERE resourceType = 'Task' AND index_name = 'status' AND index_value = 'ready'
              AND IFNULL(index_system, '') = 'http://hl7.org/fhir/task-status')
                """
            .trimIndent(),
        )
        .let {
          it.moveToFirst()
          retrievedTask = it.getString(0)
        }
    }
    migratedDatabase.close()

    assertThat(retrievedTask).isEqualTo(bedNetTask)
  }

  @Test
  fun migrate9To10_should_execute_with_no_exception(): Unit = runBlocking {
    val patient1Id = "patient-001"
    val patient1ResourceUuid = "e2c79e28-ed4d-4029-a12c-108d1eb5bedb"
    val patient1: String =
      Patient()
        .apply {
          id = patient1Id
          addName(HumanName().apply { addGiven("Brad") })
        }
        .let { iParser.encodeResourceToString(it) }

    val patient2Id = "patient-002"
    val patient2ResourceUuid = "541782b3-48f5-4c36-bd20-cae265e974e7"
    val patient2: String =
      Patient()
        .apply {
          id = patient2Id
          addName(HumanName().apply { addGiven("Alex") })
        }
        .let { iParser.encodeResourceToString(it) }

    helper.createDatabase(DB_NAME, 9).apply {
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('$patient1ResourceUuid', 'Patient', '$patient1', '$patient1');",
      )
      execSQL(
        "INSERT INTO ResourceEntity (resourceUuid, resourceType, resourceId, serializedResource) VALUES ('$patient2ResourceUuid', 'Patient', '$patient2', '$patient2');",
      )

      close()
    }

    val migratedDatabase = helper.runMigrationsAndValidate(DB_NAME, 10, true, Migration_9_10)

    val patientResult1: String?
    val patientResult2: String?

    migratedDatabase.let { database ->
      database
        .query(
          """
        SELECT a.serializedResource
        FROM ResourceEntity a
        LEFT JOIN StringIndexEntity b
        ON a.resourceUuid = b.resourceUuid AND b.index_name = 'name'
        WHERE a.resourceType = 'Patient'
        GROUP BY a.resourceUuid
        HAVING MAX(IFNULL(b.index_value,0)) >= -9223372036854775808
        ORDER BY IFNULL(b.index_value, -9223372036854775808) ASC
                """
            .trimIndent(),
        )
        .let {
          it.moveToFirst()
          patientResult1 = it.getString(0)
          it.moveToNext()
          patientResult2 = it.getString(0)
        }
    }
    migratedDatabase.close()

    assertThat(patientResult1).isEqualTo(patient2)
    assertThat(patientResult2).isEqualTo(patient1)
  }

  companion object {
    const val DB_NAME = "migration_tests.db"
  }
}

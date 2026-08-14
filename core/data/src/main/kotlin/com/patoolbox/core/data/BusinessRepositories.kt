package com.patoolbox.core.data

import com.patoolbox.core.database.dao.GearDao
import com.patoolbox.core.database.dao.InvoiceDao
import com.patoolbox.core.database.dao.SnapshotDao
import com.patoolbox.core.database.dao.WorkLogDao
import com.patoolbox.core.database.entity.GearItemEntity
import com.patoolbox.core.database.entity.InvoiceEntity
import com.patoolbox.core.database.entity.InvoiceLineEntity
import com.patoolbox.core.database.entity.SnapshotChannelEntity
import com.patoolbox.core.database.entity.SnapshotEntity
import com.patoolbox.core.database.entity.WorkLogEntity
import com.patoolbox.core.model.GearCategory
import com.patoolbox.core.model.GearItem
import com.patoolbox.core.model.GearStatus
import com.patoolbox.core.model.Invoice
import com.patoolbox.core.model.InvoiceLineItem
import com.patoolbox.core.model.Snapshot
import com.patoolbox.core.model.SnapshotChannel
import com.patoolbox.core.model.WorkLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// enum は名前で保存している。知らない名前が来ても落とさず既定値に落とす。
// 1件のデータのせいで画面全体が開かなくなる方が害が大きい。
private inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T =
    runCatching { enumValueOf<T>(this) }.getOrDefault(fallback)

// --- 機材台帳 ---

interface GearRepository {
    fun observeAll(): Flow<List<GearItem>>
    suspend fun count(): Int
    suspend fun save(item: GearItem): Long
    suspend fun delete(id: Long)
}

@Singleton
class RoomGearRepository @Inject constructor(private val dao: GearDao) : GearRepository {

    override fun observeAll(): Flow<List<GearItem>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun count(): Int = dao.count()

    override suspend fun save(item: GearItem): Long =
        dao.upsert(item.toEntity(System.currentTimeMillis()))

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

// --- スナップショット ---

interface SnapshotRepository {
    fun observeAll(): Flow<List<Snapshot>>
    fun observeWithChannels(id: Long): Flow<Snapshot?>
    suspend fun count(): Int
    suspend fun create(title: String, consoleName: String, channelCount: Int): Long
    suspend fun updateDetails(id: Long, title: String, consoleName: String, note: String)
    suspend fun saveChannel(snapshotId: Long, channel: SnapshotChannel)
    suspend fun delete(id: Long)
}

@Singleton
class RoomSnapshotRepository @Inject constructor(
    private val dao: SnapshotDao,
) : SnapshotRepository {

    override fun observeAll(): Flow<List<Snapshot>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeWithChannels(id: Long): Flow<Snapshot?> =
        combine(dao.observeById(id), dao.observeChannels(id)) { snapshot, channels ->
            snapshot?.toModel()?.copy(channels = channels.map { it.toModel() })
        }

    override suspend fun count(): Int = dao.count()

    override suspend fun create(title: String, consoleName: String, channelCount: Int): Long =
        dao.createWithChannels(
            SnapshotEntity(
                title = title,
                consoleName = consoleName,
                takenAtEpochMs = System.currentTimeMillis(),
            ),
            channelCount,
        )

    override suspend fun updateDetails(
        id: Long,
        title: String,
        consoleName: String,
        note: String,
    ) = dao.updateDetails(id, title, consoleName, note)

    override suspend fun saveChannel(snapshotId: Long, channel: SnapshotChannel) =
        dao.upsertChannel(channel.toEntity(snapshotId))

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

// --- 請求書・見積書 ---

interface InvoiceRepository {
    fun observeAll(): Flow<List<Invoice>>
    fun observeWithLines(id: Long): Flow<Invoice?>
    suspend fun count(): Int
    suspend fun create(isEstimate: Boolean, clientName: String): Long
    suspend fun save(invoice: Invoice)
    suspend fun addLine(invoiceId: Long)
    suspend fun saveLine(invoiceId: Long, line: InvoiceLineItem)
    suspend fun deleteLine(lineId: Long)
    suspend fun delete(id: Long)
}

@Singleton
class RoomInvoiceRepository @Inject constructor(
    private val dao: InvoiceDao,
) : InvoiceRepository {

    override fun observeAll(): Flow<List<Invoice>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeWithLines(id: Long): Flow<Invoice?> =
        combine(dao.observeById(id), dao.observeLines(id)) { invoice, lines ->
            invoice?.toModel()?.copy(lines = lines.map { it.toModel() })
        }

    override suspend fun count(): Int = dao.count()

    override suspend fun create(isEstimate: Boolean, clientName: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            InvoiceEntity(
                issueDateEpochMs = now,
                clientName = clientName,
                taxModeName = "EXCLUSIVE",
                taxRoundingName = "DOWN",
                isEstimate = isEstimate,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun save(invoice: Invoice) =
        dao.upsert(invoice.toEntity(System.currentTimeMillis()))

    override suspend fun addLine(invoiceId: Long) = dao.upsertLine(
        InvoiceLineEntity(
            invoiceId = invoiceId,
            taxRateName = "STANDARD",
            sortOrder = dao.nextSortOrder(invoiceId),
        ),
    )

    override suspend fun saveLine(invoiceId: Long, line: InvoiceLineItem) =
        dao.upsertLine(line.toEntity(invoiceId))

    override suspend fun deleteLine(lineId: Long) = dao.deleteLineById(lineId)

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

// --- 稼働記録 ---

interface WorkLogRepository {
    fun observeAll(): Flow<List<WorkLogEntry>>
    suspend fun count(): Int
    suspend fun save(entry: WorkLogEntry): Long
    suspend fun delete(id: Long)
}

@Singleton
class RoomWorkLogRepository @Inject constructor(
    private val dao: WorkLogDao,
) : WorkLogRepository {

    override fun observeAll(): Flow<List<WorkLogEntry>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun count(): Int = dao.count()

    override suspend fun save(entry: WorkLogEntry): Long = dao.upsert(entry.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

// --- 変換 ---

internal fun GearItemEntity.toModel() = GearItem(
    id = id,
    category = category.toEnumOr(GearCategory.OTHER),
    name = name,
    maker = maker,
    modelName = modelName,
    serial = serial,
    quantity = quantity,
    status = status.toEnumOr(GearStatus.AVAILABLE),
    note = note,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun GearItem.toEntity(now: Long) = GearItemEntity(
    id = id,
    category = category.name,
    name = name,
    maker = maker,
    modelName = modelName,
    serial = serial,
    quantity = quantity,
    status = status.name,
    note = note,
    updatedAtEpochMs = now,
)

internal fun SnapshotEntity.toModel() = Snapshot(
    id = id,
    jobId = jobId,
    title = title,
    consoleName = consoleName,
    takenAtEpochMs = takenAtEpochMs,
    note = note,
)

internal fun SnapshotChannelEntity.toModel() = SnapshotChannel(
    id = id,
    channel = channel,
    name = name,
    gain = gain,
    hpf = hpf,
    eq = eq,
    send = send,
    note = note,
)

internal fun SnapshotChannel.toEntity(snapshotId: Long) = SnapshotChannelEntity(
    id = id,
    snapshotId = snapshotId,
    channel = channel,
    name = name,
    gain = gain,
    hpf = hpf,
    eq = eq,
    send = send,
    note = note,
)

internal fun InvoiceEntity.toModel() = Invoice(
    id = id,
    jobId = jobId,
    number = number,
    issueDateEpochMs = issueDateEpochMs,
    clientName = clientName,
    subject = subject,
    issuerName = issuerName,
    registrationNumber = registrationNumber,
    taxModeName = taxModeName,
    taxRoundingName = taxRoundingName,
    note = note,
    isEstimate = isEstimate,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun Invoice.toEntity(now: Long) = InvoiceEntity(
    id = id,
    jobId = jobId,
    number = number,
    issueDateEpochMs = issueDateEpochMs,
    clientName = clientName,
    subject = subject,
    issuerName = issuerName,
    registrationNumber = registrationNumber,
    taxModeName = taxModeName,
    taxRoundingName = taxRoundingName,
    note = note,
    isEstimate = isEstimate,
    updatedAtEpochMs = now,
)

internal fun InvoiceLineEntity.toModel() = InvoiceLineItem(
    id = id,
    description = description,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    taxRateName = taxRateName,
    sortOrder = sortOrder,
)

internal fun InvoiceLineItem.toEntity(invoiceId: Long) = InvoiceLineEntity(
    id = id,
    invoiceId = invoiceId,
    description = description,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    taxRateName = taxRateName,
    sortOrder = sortOrder,
)

internal fun WorkLogEntity.toModel() = WorkLogEntry(
    id = id,
    jobId = jobId,
    title = title,
    dateEpochMs = dateEpochMs,
    startMinutesOfDay = startMinutesOfDay,
    endMinutesOfDay = endMinutesOfDay,
    breakMinutes = breakMinutes,
    rateTypeName = rateTypeName,
    rate = rate,
    multiplier = multiplier,
    transportFee = transportFee,
    note = note,
)

internal fun WorkLogEntry.toEntity() = WorkLogEntity(
    id = id,
    jobId = jobId,
    title = title,
    dateEpochMs = dateEpochMs,
    startMinutesOfDay = startMinutesOfDay,
    endMinutesOfDay = endMinutesOfDay,
    breakMinutes = breakMinutes,
    rateTypeName = rateTypeName,
    rate = rate,
    multiplier = multiplier,
    transportFee = transportFee,
    note = note,
)

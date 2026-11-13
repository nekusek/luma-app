package pl.zse.dziennik.data

import pl.zse.dziennik.model.*

object DemoProfiles {
    val accounts = listOf(
        DiaryAccount("demo:anna", "demo-zse-3ti", "Anna Kowalska", "3TI · grupa 1", "ZSE Rzeszów", "demo"),
        DiaryAccount("demo:mateusz", "demo-zse-4te", "Mateusz Nowak", "4TE · grupa 2", "ZSE Rzeszów", "demo"),
        DiaryAccount("demo:parent", "demo-zse-3ti", "Maria Kowalska", "3TI · rodzic Anny", "ZSE Rzeszów", "demo", AccountRole.PARENT),
        DiaryAccount("demo:teacher", "demo-teacher", "Piotr Wiśniewski", "Informatyka · Programowanie", "ZSE Rzeszów", "demo", AccountRole.TEACHER),
    )
}

class DemoAuthenticationProvider : AuthenticationProvider {
    override val id = "demo"
    override suspend fun authenticate(request: AuthenticationRequest): List<DiaryAccount> {
        require(request is AuthenticationRequest.Demo)
        return listOf(requireNotNull(DemoProfiles.accounts.firstOrNull { it.id == request.profileId }))
    }
    override suspend fun signOut(account: DiaryAccount) = Unit
}

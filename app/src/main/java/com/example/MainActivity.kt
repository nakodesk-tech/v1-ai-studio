package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.UIEvent
import com.example.ui.UserRole
import com.example.ui.components.EduBottomBar
import com.example.ui.components.NavTab
import com.example.ui.screens.*
import com.example.ui.theme.EduDataTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EduDataTheme {
                MainAppScreen(viewModel = viewModel, onOpenFile = ::openExportedFile)
            }
        }
    }

    private fun openExportedFile(file: File, mimeType: String) {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(this, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Generated file saved at: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onOpenFile: (File, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val role by viewModel.userRole.collectAsStateWithLifecycle()
    val schools by viewModel.allSchools.collectAsStateWithLifecycle()
    val forms by viewModel.allForms.collectAsStateWithLifecycle()
    val submissions by viewModel.allSubmissions.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val isSyncingUsers by viewModel.isSyncingUsers.collectAsStateWithLifecycle()
    val syncConfig by viewModel.syncConfig.collectAsStateWithLifecycle()
    val selectedSchool by viewModel.selectedSchool.collectAsStateWithLifecycle()

    val formCount by viewModel.schoolCount.collectAsStateWithLifecycle()
    val recordCount by viewModel.recordCount.collectAsStateWithLifecycle()
    val schoolCount by viewModel.schoolCount.collectAsStateWithLifecycle()

    val activeFormWithFields by viewModel.activeFormWithFields.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var formsSubTab by remember { mutableStateOf(0) }
    var fillingFormId by remember { mutableStateOf<String?>(null) }

    // Event listener for toasts and file exports
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UIEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UIEvent.OpenPdfFile -> {
                    Toast.makeText(context, "PDF Report generated: ${event.file.name}", Toast.LENGTH_SHORT).show()
                    onOpenFile(event.file, "application/pdf")
                }
                is UIEvent.OpenExcelFile -> {
                    Toast.makeText(context, "Excel CSV file generated: ${event.file.name}", Toast.LENGTH_SHORT).show()
                    onOpenFile(event.file, "text/csv")
                }
            }
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLogin = { email, pwd, onError ->
                viewModel.login(email, pwd, onSuccess = {}, onError = onError)
            }
        )
    } else if (fillingFormId != null) {
        BackHandler { fillingFormId = null }

        LaunchedEffect(fillingFormId) {
            viewModel.loadFormDetails(fillingFormId!!)
        }

        FormFillScreen(
            formWithFields = activeFormWithFields,
            schools = schools,
            selectedSchool = selectedSchool,
            role = role,
            currentUser = currentUser,
            onSelectSchool = { viewModel.selectSchool(it) },
            onSubmit = { values ->
                activeFormWithFields?.let { current ->
                    viewModel.submitForm(
                        formId = current.form.id,
                        formTitle = current.form.title,
                        valuesMap = values,
                        onComplete = {
                            fillingFormId = null
                            formsSubTab = 1
                            selectedTab = NavTab.FORMS
                        }
                    )
                }
            },
            onBack = { fillingFormId = null }
        )
    } else {
        if (selectedTab != NavTab.HOME) {
            BackHandler { selectedTab = NavTab.HOME }
        }

        Scaffold(
            bottomBar = {
                EduBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    role = role
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    NavTab.HOME -> {
                        HomeScreen(
                            role = role,
                            currentUser = currentUser,
                            selectedSchool = selectedSchool,
                            formCount = forms.size,
                            recordCount = submissions.size,
                            schoolCount = schools.size,
                            recentSubmissions = submissions,
                            activeForms = forms,
                            onRoleToggle = {
                                viewModel.setUserRole(
                                    if (role == UserRole.OFFICER) UserRole.HEADMASTER else UserRole.OFFICER
                                )
                            },
                            onNavigateToNewForm = {
                                selectedTab = NavTab.FORMS
                            },
                            onNavigateToFillForm = { formId ->
                                fillingFormId = formId
                            },
                            onNavigateToFormsTab = { subTab ->
                                formsSubTab = subTab
                                selectedTab = NavTab.FORMS
                            },
                            onNavigateToRecords = {
                                selectedTab = NavTab.RECORDS
                            },
                            onNavigateToSchools = {
                                selectedTab = NavTab.SCHOOLS
                            },
                            onNavigateToSettings = {
                                selectedTab = NavTab.PROFILE
                            },
                            onLogout = {
                                viewModel.logout()
                            }
                        )
                    }

                    NavTab.FORMS -> {
                        FormsScreen(
                            role = role,
                            forms = forms,
                            submissions = submissions,
                            currentUser = currentUser,
                            selectedSubTab = formsSubTab,
                            onSubTabSelected = { formsSubTab = it },
                            onSelectFormToFill = { formId ->
                                fillingFormId = formId
                            },
                            onExportFormPdf = { formId ->
                                viewModel.loadFormDetails(formId)
                                viewModel.exportActiveFormPdf()
                            },
                            onExportFormExcel = { formId ->
                                viewModel.loadFormDetails(formId)
                                viewModel.exportActiveFormExcel()
                            },
                            onCreateNewForm = { title, desc, createdBy, fields ->
                                viewModel.createNewForm(title, desc, createdBy, fields) {}
                            },
                            onImportFromExcel = { title, desc, createdBy, headersCsv ->
                                viewModel.importFormFromExcelText(title, desc, createdBy, headersCsv) {}
                            },
                            onDeleteSubmission = { formId, schoolId ->
                                viewModel.deleteSubmission(formId, schoolId)
                            },
                            onDeletePublishedForm = { formId ->
                                viewModel.deletePublishedForm(formId)
                            }
                        )
                    }

                    NavTab.RECORDS -> {
                        DashboardScreen(
                            forms = forms,
                            schools = schools,
                            submissions = submissions,
                            currentUser = currentUser,
                            role = role,
                            onExportSelectedPdf = { formId, selectedSchoolIds ->
                                viewModel.exportFormPdfForSelectedSchools(formId, selectedSchoolIds)
                            },
                            onExportSelectedExcel = { formId, selectedSchoolIds ->
                                viewModel.exportFormExcelForSelectedSchools(formId, selectedSchoolIds)
                            },
                            onLoadSubmissionValues = { submissionId, callback ->
                                viewModel.loadSubmissionValues(submissionId, callback)
                            },
                            onMarkSubmissionStatus = { submissionId, status ->
                                viewModel.markSubmissionStatus(submissionId, status)
                            }
                        )
                    }

                    NavTab.SCHOOLS -> {
                        if (role == UserRole.HEADMASTER) {
                            LaunchedEffect(Unit) { selectedTab = NavTab.HOME }
                        } else {
                            SchoolsListScreen(
                                schools = schools,
                                onUpdateSchoolContact = { schoolId, phone, email ->
                                    viewModel.updateSchoolContact(schoolId, phone, email)
                                },
                                onResetUserPassword = { udiseCode ->
                                    viewModel.resetUserPassword(udiseCode)
                                },
                                onDeleteSchool = { schoolId ->
                                    viewModel.deleteSchool(schoolId)
                                }
                            )
                        }
                    }

                    NavTab.PROFILE -> {
                        ProfileSyncScreen(
                            currentRole = role,
                            syncConfig = syncConfig,
                            currentUser = currentUser,
                            allUsers = allUsers,
                            isSyncingUsers = isSyncingUsers,
                            onSyncUsers = { viewModel.syncUsers() },
                            onSaveSyncConfig = { email, sheetId, webhookUrl, autoSync ->
                                viewModel.saveGoogleSyncSettings(email, sheetId, webhookUrl, autoSync)
                            },
                            onRegisterOfficer = { offId, name, desig, phone, email, pass, onSuccess, onError ->
                                viewModel.registerOfficerUser(offId, name, desig, phone, email, pass, onSuccess, onError)
                            },
                            onRegisterSchool = { udise, name, hmName, phone, email, pass, userRole, onSuccess, onError ->
                                viewModel.registerSchoolUser(udise, name, hmName, phone, email, pass, userRole, onSuccess, onError)
                            },
                            onCreateAccount = { role, fullName, email, password, confirmPassword, schoolName, udiseNumber, onSuccess, onError ->
                                viewModel.createNewAccount(role, fullName, email, password, confirmPassword, schoolName, udiseNumber, onSuccess, onError)
                            },
                            onChangePassword = { oldPass, newPass, confirmPass, onSuccess, onError ->
                                viewModel.changeCurrentUserPassword(oldPass, newPass, confirmPass, onSuccess, onError)
                            },
                            onResetUserPassword = { udiseCode ->
                                viewModel.resetUserPassword(udiseCode)
                            },
                            onDeleteUser = { udiseCode ->
                                viewModel.deleteUser(udiseCode)
                            },
                            onUpdateUserInfo = { udiseCode, name, phone, email, schoolName, udiseNumber ->
                                viewModel.updateUserInfo(udiseCode, name, phone, email, schoolName, udiseNumber)
                            },
                            onLogout = {
                                viewModel.logout()
                            }
                        )
                    }
                }
            }
        }
    }
}

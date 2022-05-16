package com.example.projemanag.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projemanag.activities.*
import com.example.projemanag.models.Board
import com.example.projemanag.models.User
import com.example.projemanag.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId()).set(
            userInfo,
            SetOptions.merge()
        ).addOnSuccessListener {
            activity.userRegisteredSuccess()
        }.addOnFailureListener { e ->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error writing")
        }
    }

    fun createBoard(activity: CreateBoardActivity, boardInfo: Board) {
        mFireStore.collection(Constants.BOARDS).document().set(
            boardInfo,
            SetOptions.merge()
        ).addOnSuccessListener {
            Toast.makeText(activity, "board created successfully", Toast.LENGTH_SHORT).show()
            activity.boardCreatedSuccessfully()
        }.addOnFailureListener { _ ->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error board")
        }
    }

    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId()).update(userHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "profile update")
                Toast.makeText(activity, "profile updated", Toast.LENGTH_SHORT).show()
                when (activity) {
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity -> {
                        activity.profileUpdateSuccess()
                    }
                }
            }.addOnFailureListener {
                when (activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Toast.makeText(activity, "error at profile updated", Toast.LENGTH_SHORT).show()
            }
    }

    fun getBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val boardList: ArrayList<Board> = ArrayList()
                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }
                activity.populateBoardsListToUI(boardList)
            }.addOnFailureListener {
                activity.hideProgressDialog()
            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)
                if (loggedInUser != null) {
                    when (activity) {
                        is SignInActivity -> {
                            activity.signInSuccess(loggedInUser)
                        }
                        is MainActivity -> {
                            activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                        }
                        is MyProfileActivity -> {
                            activity.setUserDataInUI(loggedInUser)
                        }
                    }
                }
            }.addOnFailureListener { e ->
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error reading", e)
            }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
            }.addOnFailureListener {
                activity.hideProgressDialog()
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore
            .collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                if (activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                else if (activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }.addOnFailureListener {
                if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                else if (activity is CardDetailsActivity)
                    activity.hideProgressDialog()
            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore
            .collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val usersList: ArrayList<User> = ArrayList()
                for (i in document.documents) {
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if (activity is MembersActivity)
                    activity.setupMembersList(usersList)
                else if (activity is TaskListActivity)
                    activity.boardMembersDetailsList(usersList)
            }.addOnFailureListener {
                if (activity is MembersActivity)
                    activity.hideProgressDialog()
                else if (activity is TaskListActivity)
                    activity.hideProgressDialog()
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String) {
        mFireStore
            .collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener { document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }
            }.addOnFailureListener { activity.hideProgressDialog() }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User) {
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore
            .collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener {
                activity.hideProgressDialog()
            }
    }
}

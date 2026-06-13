package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val SRINAGAR_AREAS = listOf(
    "Lal Chowk", "Rajbagh", "Jawahar Nagar", "Karan Nagar", 
    "Gau Kadal", "Maisuma", "Hazratbal", "Soura", "Nishat", 
    "Shalimar", "Khanyar", "Nowhatta", "Hawal", "Zadibal", 
    "Bemina", "Qamarwari", "Batamaloo", "Sanat Nagar", "Hyderpora", 
    "Chanapora", "Natipora", "Bagh-e-Mehtab", "Nowgam", "Parraypora",
    "Barzulla", "Aloochi Bagh", "Chattabal", "Safa Kadal"
).sorted()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompleteAddressScreen(
    viewModel: NeighborCartViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var selectedArea by remember { mutableStateOf("") }
    var isAreaDropdownExpanded by remember { mutableStateOf(false) }
    var addressDetails by remember { mutableStateOf("") }
    
    // Internal user info state (from session)
    val customerName = viewModel.userName.ifEmpty { "Guest User" }
    val customerPhone = viewModel.userPhone.ifEmpty { "+91 98765-43210" }

    var selectedAddressType by remember { mutableStateOf(0) }
    var isSavingInProgress by remember { mutableStateOf(false) }

    var gpsCoordinates by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            if (fineLocationGranted || coarseLocationGranted) {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            gpsCoordinates = "${location.latitude}, ${location.longitude}"
                            Toast.makeText(context, "GPS Coordinates locked securely! 📍", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "GPS Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Address & Dispatch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showAddAddressScreen = false }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    // Safety / Privacy Disclaimer
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Secure", tint = BrandGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Your exact location data is encrypted & secure",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSavingInProgress = true
                                delay(1200) // Simulated processing
                                val fullAddress = "$addressDetails, $selectedArea, Srinagar, J&K" + (if (gpsCoordinates != null) " [GPS: $gpsCoordinates]" else "")
                                
                                viewModel.saveUserSession(
                                    phone = customerPhone,
                                    name = customerName,
                                    address = fullAddress,
                                    typeIndex = selectedAddressType
                                )
                                isSavingInProgress = false
                                viewModel.showAddAddressScreen = false
                            }
                        },
                        enabled = selectedArea.isNotEmpty() && addressDetails.trim().isNotEmpty() && !isSavingInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        if (isSavingInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome, $customerName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandBlack
                )
                Text(
                    text = "Let's setup your Hyper-Local identity for quick dispatch.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Area Dropdown
            item {
                Column {
                    Text(
                        text = "Srinagar Local Sector",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = isAreaDropdownExpanded,
                        onExpandedChange = { isAreaDropdownExpanded = !isAreaDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedArea,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select your area from list", color = Color.Gray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAreaDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = BrandBlack,
                                unfocusedTextColor = BrandBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = isAreaDropdownExpanded,
                            onDismissRequest = { isAreaDropdownExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 300.dp)
                        ) {
                            SRINAGAR_AREAS.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area, fontSize = 14.sp, color = BrandBlack) },
                                    onClick = {
                                        selectedArea = area
                                        isAreaDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Manual Address Details
            item {
                Column {
                    Text(
                        text = "House No. / Street Lane / Landmark",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = addressDetails,
                        onValueChange = { addressDetails = it },
                        placeholder = { Text("e.g. Block C, Next to Central Mosque...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = BrandBlack,
                            unfocusedTextColor = BrandBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Free GPS Autofill Button
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = "GPS", tint = BrandGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero-Cost GPS Telemetry",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                        
                        Text(
                            text = "Tap to grab exact latitude/longitude silently for our dashboard.",
                            fontSize = 12.sp,
                            color = BrandBlack.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )

                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (gpsCoordinates != null) "Telemetry Secured ✓" else "Authorize GPS Pulse", fontWeight = FontWeight.Black)
                        }
                        
                        AnimatedVisibility(visible = gpsCoordinates != null) {
                            Text(
                                text = "Coordinates: $gpsCoordinates",
                                fontSize = 10.sp,
                                color = BrandGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Address Type Selection (Home, Work)
            item {
                Column {
                    Text(
                        text = "Save Address As",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf("Home", "Work", "Other")
                        types.forEachIndexed { index, title ->
                            val isSelected = selectedAddressType == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) BrandGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) BrandGreen else Color.LightGray,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedAddressType = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandGreen else Color.Gray
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp)) // padding for bottom bar
            }
        }
    }
}

package com.antigravity.webtoonhub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.antigravity.webtoonhub.api.WebtoonItem
import com.antigravity.webtoonhub.api.WebtoonScraper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebtoonDashboard(onWebtoonClick = { url ->
                        val intent = Intent(this, WebtoonViewerActivity::class.java).apply {
                            putExtra("URL", url)
                        }
                        startActivity(intent)
                    })
                }
            }
        }
    }
}

@Composable
fun WebtoonDashboard(onWebtoonClick: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var webtoonList by remember { mutableStateOf<List<WebtoonItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val scraper = WebtoonScraper()
            val naverWebtoons = scraper.getNaverWebtoonList()
            val kakaoWebtoons = scraper.getKakaoWebtoonList()
            webtoonList = kakaoWebtoons + naverWebtoons
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar()
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(webtoonList) { webtoon ->
                    WebtoonCard(webtoon, onWebtoonClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar() {
    TopAppBar(
        title = {
            Text("WebtoonHub", fontWeight = FontWeight.Bold)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
    )
}

@Composable
fun WebtoonCard(webtoon: WebtoonItem, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { onClick(webtoon.linkUrl) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AsyncImage(
                model = webtoon.thumbnailUrl,
                contentDescription = webtoon.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = webtoon.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = if (webtoon.platform == "NAVER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary) {
                        Text(webtoon.platform, fontSize = 8.sp)
                    }
                    if (webtoon.isUpdate) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("UP", fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

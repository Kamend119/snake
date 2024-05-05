package com.example.gamesnake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "Beginning/{score}") {
                composable("Beginning/{score}") { Beginning( navController, it.arguments?.getString("score") ?: "0" ) }
                composable("Result/{result}/{score}") { Result(navController, it.arguments?.getString("result") ?: "Lose", it.arguments?.getString("score") ?: "0") }
                composable("Game") { Game( navController ) }
            }
        }
    }
}

@Composable
fun Beginning(navController: NavHostController, score: String){
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Snake Game", fontSize = 40.sp)
        Button(onClick = { navController.navigate("Game") }) {
            Text("Start")
        }
        Text("High Score: $score")
    }
}

enum class Result { Win, Lose, Game }

@Composable
fun Result(navController: NavHostController, result: String, score: String) {
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (result == "Win") "You Win" else "You Lose", fontSize = 40.sp)
        Button(onClick = {
            navController.navigate("Beginning/${if (result == "Win") score else "0"}")
        }) {
            Text("Back")
        }
        Text("Score: ${if (result == "Win") score else "0"}")
    }
}

enum class Direction { Up, Down, Left, Right }
class Points( var x: Int, var y: Int )

class Snake{
    private var direction = Direction.Right
    private var body = mutableListOf(Points(3,3), Points(2,3), Points(1,3))

    fun move(apple: Apple): Result {
        val head = body[0]
        val newHead = when (direction) {
            Direction.Up -> Points(head.x, head.y - 1)
            Direction.Left -> Points(head.x - 1, head.y)
            Direction.Down -> Points(head.x, head.y + 1)
            Direction.Right -> Points(head.x + 1, head.y)
        }

        if (meetBorder(newHead) != Result.Game) {
            return Result.Lose
        }
        if (biteSelf() != Result.Game) {
            return Result.Lose
        }

        if (newHead.x == apple.position.x && newHead.y == apple.position.y) {
            body.add(0, newHead)
            apple.regen(body)
            return if (body.size == 10) Result.Win else Result.Game
        }
        else{
            body.add(0, newHead)
            body.removeAt(body.lastIndex)
        }

        return Result.Game
    }


    fun draw(canvas: DrawScope){
        body.forEach { segment ->
            canvas.drawRect(color = Color.Green, topLeft = Offset(segment.x * 95f,
                segment.y * 95f), size = Size(95f, 95f), style = Fill)
        }
    }

    fun changeDirection(newDirection: Direction){
        when (newDirection) {
            Direction.Up -> if (direction != Direction.Down) direction = newDirection
            Direction.Left -> if (direction != Direction.Right) direction = newDirection
            Direction.Down -> if (direction != Direction.Up) direction = newDirection
            Direction.Right -> if (direction != Direction.Left) direction = newDirection
        }
    }

    private fun biteSelf(): Result {
        val head = body.first()
        for (segmentIndex in 1 until body.size) {
            val segment = body[segmentIndex]
            if (segment.x == head.x && segment.y == head.y) {
                return Result.Lose
            }
        }
        return Result.Game
    }


    private fun meetBorder(head: Points): Result {
        return if (head.x < 0 || head.x > 9 || head.y < 0 || head.y > 9) Result.Lose
        else Result.Game
    }
}

class Apple{
    var position = Points(Random.nextInt(0,10), Random.nextInt(0,10))

    fun draw(canvas: DrawScope){
        canvas.drawRect(color = Color.Red, topLeft = Offset(position.x * 95f, position.y * 95f),
            size = Size(95f, 95f), style = Fill)
    }

    fun regen(snake: MutableList<Points>){
        position = Points(Random.nextInt(0,10), Random.nextInt(0,10))
        snake.forEach { segment ->
            if (segment.x == position.x && segment.y == position.y)
                position = Points(Random.nextInt(0,10), Random.nextInt(0,10))
        }
    }
}

class Board{
    fun draw(canvas: DrawScope){
        for (i in 0..10){
            canvas.drawLine(Color.Black, start = Offset(i*95f,0f),
                end = Offset(i*95f, 950f), strokeWidth = 10f)
            canvas.drawLine(Color.Black, start = Offset(0f, i*95f),
                end = Offset(950f, i*95f), strokeWidth = 10f)
        }
    }
}

@Composable
fun Game(navController: NavHostController){
    var result by remember { mutableStateOf(Result.Game) }
    var satiety by remember { mutableIntStateOf(50) }
    val snake by remember { mutableStateOf(Snake()) }
    val apple by remember { mutableStateOf(Apple()) }
    val board by remember { mutableStateOf(Board()) }
    var frame by remember { mutableIntStateOf(0) }
    val text = rememberTextMeasurer()

    LaunchedEffect(frame) {
        while (result == Result.Game) {
            delay(500)
            val moveResult = snake.move(apple)
            if (moveResult != Result.Game) {
                result = moveResult
                if (result != Result.Game) {
                    val res = when(result){
                        Result.Lose -> "Lose"
                        Result.Win -> "Win"
                        else -> "Game"
                    }
                    navController.navigate("Result/${res}/${satiety}")
                }
            }
            frame++
        }
    }


    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("Points: $satiety", fontSize = 30.sp)
        Box(modifier = Modifier
            .padding(20.dp)
            .border(3.dp, Color.Black)
            .width(350.dp)
            .height(350.dp)){
            Canvas(
                modifier = Modifier
                    .border(2.dp, Color.Blue)
                    .fillMaxSize()
            ) {
                snake.draw(this)
                apple.draw(this)
                board.draw(this)
                drawText(text, "$frame", style = androidx.compose.ui.text.TextStyle(Color.Transparent))
            }
        }

        Box (modifier = Modifier
            .padding(5.dp)
            .border(3.dp, Color.Black)){
            IconButton(onClick = {
                satiety--
                snake.changeDirection(Direction.Up)
            }){ Icon(Icons.Default.KeyboardArrowUp, "") }
        }
        Row{
            Box (modifier = Modifier
                .padding(5.dp)
                .border(3.dp, Color.Black)){
                IconButton(onClick = {
                    satiety--
                    snake.changeDirection(Direction.Left)
                }){ Icon(Icons.Default.KeyboardArrowLeft, "") }
            }
            Box (modifier = Modifier
                .padding(5.dp)
                .border(3.dp, Color.Black)){
                IconButton(onClick = {
                    satiety--
                    snake.changeDirection(Direction.Down)
                }){ Icon(Icons.Default.KeyboardArrowDown, "") }
            }
            Box (modifier = Modifier
                .padding(5.dp)
                .border(3.dp, Color.Black)){
                IconButton(onClick = {
                    satiety--
                    snake.changeDirection(Direction.Right)
                }){ Icon(Icons.Default.KeyboardArrowRight, "") }
            }
        }
    }
}
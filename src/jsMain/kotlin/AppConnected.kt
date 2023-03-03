import channel.DMsPanel
import data.TimestampedId
import react.FC
import react.Props

val AppConnected = FC<Props> { _ ->
    UserCard { id = TimestampedId(389384133) }
    DMsPanel { }
}
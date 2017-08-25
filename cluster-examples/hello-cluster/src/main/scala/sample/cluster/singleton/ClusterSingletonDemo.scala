package sample.cluster.singleton

/**
  * Created by zhengqh on 17/8/15.
  */
object ClusterSingletonDemo extends App{

  SingletonActor.create(2551)    //seed-node

  SingletonActor.create(0)   //ClusterSingletonManager node
  SingletonActor.create(0)
  SingletonActor.create(0)
  SingletonActor.create(0)

  SingletonUser.create     //ClusterSingletonProxy node

}

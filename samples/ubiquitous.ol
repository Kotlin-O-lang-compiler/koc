//class A is
//    method foo
//    method foo is end
//    method x: A is
//        return this
//    end
//    method xx(arg: Integer): A is
//        return this
//    end
//    method xxx is
//        var tmp: xx(1).x().foo()
//    end
//end

class Main is
    this is
        var a: A()

        a.foo()
    end
end

class A is
    method foo is end
end
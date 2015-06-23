class HWError(EnvironmentError):
     def __call__(self, *args):
        return self.__class__(*(self.args + args))

class TargetLost(IOError):
    def __call__(self, *args):
        return self.__class__(*(self.args + args))